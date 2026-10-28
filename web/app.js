document.addEventListener('DOMContentLoaded', () => {
    // -------------------------------------------------------------
    // 1. Phone Simulator Tab Switching
    // -------------------------------------------------------------
    const navItems = document.querySelectorAll('.sim-nav-item');
    const screenViews = document.querySelectorAll('.screen-view');

    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const tab = item.getAttribute('data-tab');
            navItems.forEach(i => i.classList.remove('active'));
            screenViews.forEach(v => v.classList.remove('active'));

            item.classList.add('active');
            const targetView = document.getElementById(`view-${tab}`);
            if (targetView) targetView.classList.add('active');
        });
    });

    // -------------------------------------------------------------
    // 2. Interactive 24-Hour Sleep Dial Math
    // -------------------------------------------------------------
    const dialTrack = document.getElementById('dial-track');
    const dialArc = document.getElementById('dial-arc');
    const dialDurationText = document.getElementById('dial-duration-text');
    const bedtimeVal = document.getElementById('bedtime-val');
    const wakeupVal = document.getElementById('wakeup-val');
    let isDraggingDial = false;
    let sleepHours = 8.0;

    function updateDial(angleDeg) {
        // Map angle 0 - 360 to sleep hours (4h - 12h)
        const normalized = ((angleDeg % 360) + 360) % 360;
        sleepHours = Math.round((4 + (normalized / 360) * 8) * 2) / 2;
        dialDurationText.textContent = `${sleepHours}h`;

        // Circumference for r=80 is ~502
        const circumference = 2 * Math.PI * 80;
        const arcLength = (sleepHours / 24) * circumference;
        dialArc.style.strokeDasharray = `${arcLength} ${circumference}`;

        // Bedtime calculation
        const wakeHour = 7;
        let bedHour = (wakeHour - sleepHours + 24) % 24;
        const isPM = bedHour >= 12;
        const displayBedHour = bedHour > 12 ? bedHour - 12 : (bedHour === 0 ? 12 : bedHour);
        const bedMin = (bedHour % 1) * 60;
        bedtimeVal.textContent = `${Math.floor(displayBedHour)}:${bedMin === 0 ? '00' : '30'} ${isPM ? 'pm' : 'am'}`;
    }

    if (dialTrack) {
        dialTrack.addEventListener('mousedown', (e) => {
            isDraggingDial = true;
            handleDialMove(e);
        });
        window.addEventListener('mousemove', (e) => {
            if (isDraggingDial) handleDialMove(e);
        });
        window.addEventListener('mouseup', () => { isDraggingDial = false; });

        // Touch support for mobile
        dialTrack.addEventListener('touchstart', (e) => {
            isDraggingDial = true;
            handleDialMove(e.touches[0]);
        });
        window.addEventListener('touchmove', (e) => {
            if (isDraggingDial) handleDialMove(e.touches[0]);
        });
        window.addEventListener('touchend', () => { isDraggingDial = false; });
    }

    function handleDialMove(e) {
        const rect = dialTrack.getBoundingClientRect();
        const centerX = rect.left + rect.width / 2;
        const centerY = rect.top + rect.height / 2;
        const dx = e.clientX - centerX;
        const dy = e.clientY - centerY;
        let angle = Math.atan2(dy, dx) * (180 / Math.PI) + 90;
        updateDial(angle);
    }

    // Initialize with 8h
    updateDial(180);

    // Sleep toggle button
    const sleepToggleBtn = document.getElementById('sim-sleep-toggle');
    const sleepToggleLabel = document.getElementById('sleep-toggle-label');
    let isSleepActive = false;

    if (sleepToggleBtn) {
        sleepToggleBtn.addEventListener('click', () => {
            isSleepActive = !isSleepActive;
            if (isSleepActive) {
                sleepToggleLabel.textContent = "Turn off Sleep Mode →";
                sleepToggleBtn.style.background = "var(--accent-coral)";
            } else {
                sleepToggleLabel.textContent = "Turn on Sleep Mode →";
                sleepToggleBtn.style.background = "var(--accent-purple)";
            }
        });
    }

    // -------------------------------------------------------------
    // 3. Full App Blocking Countdown Timer Stepper
    // -------------------------------------------------------------
    let blockMinutes = 30;
    let blockTimerInterval = null;
    let isBlockingActive = false;
    let remainingSeconds = blockMinutes * 60;

    const blockTimerDisplay = document.getElementById('block-timer-display');
    const stepValText = document.getElementById('step-val-text');
    const stepMinus = document.getElementById('step-minus');
    const stepPlus = document.getElementById('step-plus');
    const simBlockStartBtn = document.getElementById('sim-block-start-btn');
    const presetChips = document.querySelectorAll('.preset-chip');

    function updateTimerDisplay() {
        const m = Math.floor(remainingSeconds / 60);
        const s = remainingSeconds % 60;
        blockTimerDisplay.textContent = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }

    if (stepMinus) {
        stepMinus.addEventListener('click', () => {
            if (isBlockingActive) return;
            if (blockMinutes > 5) blockMinutes -= 5;
            remainingSeconds = blockMinutes * 60;
            stepValText.textContent = `${blockMinutes} min`;
            simBlockStartBtn.textContent = `Start ${blockMinutes} min block`;
            updateTimerDisplay();
        });
    }

    if (stepPlus) {
        stepPlus.addEventListener('click', () => {
            if (isBlockingActive) return;
            blockMinutes += 5;
            remainingSeconds = blockMinutes * 60;
            stepValText.textContent = `${blockMinutes} min`;
            simBlockStartBtn.textContent = `Start ${blockMinutes} min block`;
            updateTimerDisplay();
        });
    }

    presetChips.forEach(chip => {
        chip.addEventListener('click', () => {
            if (isBlockingActive) return;
            presetChips.forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            blockMinutes = parseInt(chip.getAttribute('data-time'), 10);
            remainingSeconds = blockMinutes * 60;
            stepValText.textContent = `${blockMinutes} min`;
            simBlockStartBtn.textContent = `Start ${blockMinutes} min block`;
            updateTimerDisplay();
        });
    });

    if (simBlockStartBtn) {
        simBlockStartBtn.addEventListener('click', () => {
            if (isBlockingActive) {
                // Stop block
                clearInterval(blockTimerInterval);
                isBlockingActive = false;
                simBlockStartBtn.textContent = `Start ${blockMinutes} min block`;
                simBlockStartBtn.style.background = "var(--accent-blue)";
                remainingSeconds = blockMinutes * 60;
                updateTimerDisplay();
            } else {
                // Start block
                isBlockingActive = true;
                simBlockStartBtn.textContent = "Stop Focus Block";
                simBlockStartBtn.style.background = "var(--accent-coral)";
                blockTimerInterval = setInterval(() => {
                    if (remainingSeconds > 0) {
                        remainingSeconds--;
                        updateTimerDisplay();
                    } else {
                        clearInterval(blockTimerInterval);
                        isBlockingActive = false;
                        simBlockStartBtn.textContent = `Start ${blockMinutes} min block`;
                        simBlockStartBtn.style.background = "var(--accent-blue)";
                    }
                }, 1000);
            }
        });
    }

    // -------------------------------------------------------------
    // 4. Interactive Feed Stripper Live Sandbox
    // -------------------------------------------------------------
    const sandboxToggle = document.getElementById('sandbox-filter-toggle');
    const sandboxStatusLabel = document.getElementById('sandbox-status-label');
    const cardReel = document.getElementById('card-reel');
    const cardAd = document.getElementById('card-ad');
    const todayStrippedBadge = document.getElementById('today-stripped-badge');
    const analyticsBlocksNum = document.getElementById('analytics-blocks-num');
    const analyticsShortsTrapped = document.getElementById('analytics-shorts-trapped');

    function applyFilterState(active) {
        if (active) {
            sandboxStatusLabel.innerHTML = 'SocialBreak Filter: <strong class="text-emerald">Active</strong>';
            if (cardReel) cardReel.classList.add('stripped');
            if (cardAd) cardAd.classList.add('stripped');
            if (todayStrippedBadge) todayStrippedBadge.textContent = "16 stripped";
            if (analyticsBlocksNum) analyticsBlocksNum.textContent = "16";
            if (analyticsShortsTrapped) analyticsShortsTrapped.textContent = "16";
        } else {
            sandboxStatusLabel.innerHTML = 'SocialBreak Filter: <strong class="text-coral">Disabled (Doomscroll)</strong>';
            if (cardReel) cardReel.classList.remove('stripped');
            if (cardAd) cardAd.classList.remove('stripped');
            if (todayStrippedBadge) todayStrippedBadge.textContent = "0 stripped";
            if (analyticsBlocksNum) analyticsBlocksNum.textContent = "0";
            if (analyticsShortsTrapped) analyticsShortsTrapped.textContent = "0";
        }
    }

    if (sandboxToggle) {
        sandboxToggle.addEventListener('change', () => {
            applyFilterState(sandboxToggle.checked);
        });
        // Initial state is active
        applyFilterState(true);
    }

    // -------------------------------------------------------------
    // 5. Playable 4-Stage Friction Gauntlet
    // -------------------------------------------------------------
    const COMMITMENT_PHRASE = "I am breaking my digital wellness commitment";
    const gauntletStageBadge = document.getElementById('gauntlet-stage-badge');
    const gauntletS1 = document.getElementById('gauntlet-s1');
    const gauntletS2 = document.getElementById('gauntlet-s2');
    const gauntletS3 = document.getElementById('gauntlet-s3');
    const gauntletS4 = document.getElementById('gauntlet-s4');
    const gauntletSuccess = document.getElementById('gauntlet-success');

    const inputPhrase = document.getElementById('gauntlet-input-phrase');
    const btnS1 = document.getElementById('gauntlet-btn-s1');

    const mathQuestionText = document.getElementById('math-question-text');
    const mathProgress = document.getElementById('math-progress');
    const inputMath = document.getElementById('gauntlet-input-math');
    const btnS2 = document.getElementById('gauntlet-btn-s2');

    const cooldownDisplay = document.getElementById('cooldown-timer-display');
    const btnS4 = document.getElementById('gauntlet-btn-s4');
    const resetGauntletBtn = document.getElementById('gauntlet-reset-btn');
    const openGauntletBtn = document.getElementById('open-gauntlet-btn');

    let currentMathAnswer = 0;
    let mathSolvedCount = 0;
    let cooldownTimer = 15;
    let cooldownInterval = null;

    function generateMathQuestion() {
        const a = Math.floor(Math.random() * 9) + 6; // 6 - 14
        const b = Math.floor(Math.random() * 8) + 4; // 4 - 11
        const c = Math.floor(Math.random() * 7) + 3; // 3 - 9
        currentMathAnswer = (a * b) - c;
        mathQuestionText.textContent = `(${a} × ${b}) - ${c} = ?`;
        inputMath.value = '';
    }

    // Stage 1: Typing check
    if (inputPhrase) {
        inputPhrase.addEventListener('input', () => {
            if (inputPhrase.value.trim().toLowerCase() === COMMITMENT_PHRASE.toLowerCase()) {
                btnS1.disabled = false;
                btnS1.classList.remove('btn-secondary');
                btnS1.classList.add('btn-amber');
            } else {
                btnS1.disabled = true;
            }
        });
    }

    if (btnS1) {
        btnS1.addEventListener('click', () => {
            gauntletS1.classList.remove('active');
            gauntletS2.classList.add('active');
            gauntletStageBadge.textContent = "Stage 2 of 4";
            mathSolvedCount = 0;
            mathProgress.textContent = "0/5";
            generateMathQuestion();
        });
    }

    // Stage 2: Math checks
    if (btnS2) {
        btnS2.addEventListener('click', () => {
            const entered = parseInt(inputMath.value.trim(), 10);
            if (entered === currentMathAnswer) {
                mathSolvedCount++;
                mathProgress.textContent = `${mathSolvedCount}/5`;
                if (mathSolvedCount >= 5) {
                    // Transition to Stage 3
                    gauntletS2.classList.remove('active');
                    gauntletS3.classList.add('active');
                    gauntletStageBadge.textContent = "Stage 3 of 4";
                    startCooldown();
                } else {
                    generateMathQuestion();
                }
            } else {
                inputMath.style.borderColor = "var(--accent-coral)";
                setTimeout(() => { inputMath.style.borderColor = "var(--border-subtle)"; }, 800);
            }
        });
    }

    // Stage 3: Cooldown countdown
    function startCooldown() {
        cooldownTimer = 15;
        cooldownDisplay.textContent = `${cooldownTimer}s`;
        clearInterval(cooldownInterval);
        cooldownInterval = setInterval(() => {
            if (cooldownTimer > 1) {
                cooldownTimer--;
                cooldownDisplay.textContent = `${cooldownTimer}s`;
            } else {
                clearInterval(cooldownInterval);
                gauntletS3.classList.remove('active');
                gauntletS4.classList.add('active');
                gauntletStageBadge.textContent = "Stage 4 of 4";
            }
        }, 1000);
    }

    // Stage 4: Final confirm
    if (btnS4) {
        btnS4.addEventListener('click', () => {
            gauntletS4.classList.remove('active');
            gauntletSuccess.classList.add('active');
            gauntletStageBadge.textContent = "Unlocked";
            gauntletStageBadge.className = "badge badge-pulse";
        });
    }

    // Reset Gauntlet
    if (resetGauntletBtn) {
        resetGauntletBtn.addEventListener('click', () => {
            gauntletSuccess.classList.remove('active');
            gauntletS1.classList.add('active');
            gauntletStageBadge.textContent = "Stage 1 of 4";
            gauntletStageBadge.className = "badge badge-amber";
            inputPhrase.value = '';
            btnS1.disabled = true;
        });
    }

    // Link "Hard Mode" from sim to scroll to Gauntlet
    if (openGauntletBtn) {
        openGauntletBtn.addEventListener('click', () => {
            document.getElementById('gauntlet').scrollIntoView({ behavior: 'smooth' });
        });
    }

    // -------------------------------------------------------------
    // 6. Zero-Server Subsystems: Web Filter Chips, Grayscale & Backup
    // -------------------------------------------------------------
    const filterChips = document.querySelectorAll('.filter-chip');
    filterChips.forEach(chip => {
        chip.addEventListener('click', () => {
            chip.classList.toggle('active');
        });
    });

    const grayscaleToggle = document.getElementById('sim-grayscale-toggle');
    const phoneScreen = document.getElementById('phone-screen');
    if (grayscaleToggle && phoneScreen) {
        grayscaleToggle.addEventListener('change', () => {
            if (grayscaleToggle.checked) {
                phoneScreen.style.filter = 'grayscale(1)';
            } else {
                phoneScreen.style.filter = 'none';
            }
        });
        // Initial state is grayscale enabled
        phoneScreen.style.filter = 'grayscale(1)';
    }

    const exportBackupBtn = document.getElementById('sim-export-backup-btn');
    const importBackupBtn = document.getElementById('sim-import-backup-btn');
    const backupStatusMsg = document.getElementById('backup-status-msg');

    if (exportBackupBtn) {
        exportBackupBtn.addEventListener('click', () => {
            const simulatedBackup = {
                version: 1,
                app: "SocialBreak",
                algorithm: "AES-256-GCM",
                zeroServer: true,
                timestamp: Date.now(),
                exportedDate: new Date().toISOString(),
                payload: "7f8b91a2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8"
            };
            const blob = new Blob([JSON.stringify(simulatedBackup, null, 2)], { type: "application/json" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `socialbreak_backup_${new Date().toISOString().slice(0, 10)}.sbk`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);

            if (backupStatusMsg) {
                backupStatusMsg.textContent = "✓ Exported AES-256 local backup (.sbk)";
                setTimeout(() => { backupStatusMsg.textContent = ""; }, 3500);
            }
        });
    }

    if (importBackupBtn) {
        importBackupBtn.addEventListener('click', () => {
            if (backupStatusMsg) {
                backupStatusMsg.textContent = "✓ Backup verified & restored (0 network pings)";
                setTimeout(() => { backupStatusMsg.textContent = ""; }, 3500);
            }
        });
    }
});
