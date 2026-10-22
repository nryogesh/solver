/* ================================================================
   Smart Equation Solver — Frontend JavaScript
   Handles OCR upload, equation classification, and solving via REST API
   ================================================================ */

const API_BASE = '/api';
let selectedFile = null;
let lastClassification = null;

// ═══════════════════════════════════════════════════════════════════
// DRAG & DROP SETUP
// ═══════════════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', () => {
    // Hard guarantee spinner is removed on load (prevents "stuck overlay" UX).
    hideLoading();
    const spinner = document.getElementById('loadingSpinner');
    if (spinner) spinner.remove();

    const dropZone = document.getElementById('dropZone');
    const imageUpload = document.getElementById('imageUpload');

    imageUpload.addEventListener('change', (e) => {
        if (e.target.files.length > 0) handleFileSelect(e.target.files[0]);
    });

    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('drag-over');
    });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('drag-over'));
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('drag-over');
        if (e.dataTransfer.files.length > 0) handleFileSelect(e.dataTransfer.files[0]);
    });
    dropZone.addEventListener('click', (e) => {
        if (e.target.tagName !== 'LABEL' && e.target.tagName !== 'INPUT') imageUpload.click();
    });
});

function handleFileSelect(file) {
    const allowedExtensions = /\.(png|jpe?g)$/i;
    const allowedTypes = ['image/png', 'image/jpg', 'image/jpeg'];
    if (!allowedExtensions.test(file.name) || (file.type && !allowedTypes.includes(file.type))) {
        showError('Invalid format. Please upload PNG, JPG, or JPEG.');
        return;
    }
    if (file.size > 5 * 1024 * 1024) { showError('File too large. Maximum size is 5MB.'); return; }
    selectedFile = file;
    document.getElementById('dropZone').classList.add('has-file');
    document.getElementById('fileName').classList.remove('d-none');
    document.getElementById('fileNameText').textContent = file.name + ' (' + formatFileSize(file.size) + ')';
    showSuccess('File "' + file.name + '" selected. Click Extract Text to run OCR.');
}

// ═══════════════════════════════════════════════════════════════════
// OCR EXTRACTION
// ═══════════════════════════════════════════════════════════════════

async function performOCR() {
    if (!selectedFile) { showError('Please select an image file first.'); return; }
    showLoading('Extracting equation text from image...');
    try {
        const formData = new FormData();
        formData.append('file', selectedFile);
        const response = await fetch(API_BASE + '/ocr/extract', { method: 'POST', body: formData });
        let data;
        try {
            data = await response.json();
        } catch (parseError) {
            throw new Error('The OCR service returned an unreadable response. Check the server logs.');
        }
        if (response.ok && data.success) {
            document.getElementById('ocrOutput').value = data.extractedText;
            showSuccess('Text extracted successfully! Review and correct if needed.');
            if (!data.isValidEquation) showError('Warning: Extracted text may not be a valid equation. Please verify.');
        } else {
            showError('OCR failed: ' + (data.message || data.errorCode || 'The server could not process this image.'));
        }
    } catch (err) {
        showError('Network error during OCR: ' + err.message);
    } finally {
        hideLoading();
    }
}


function useOCRText() {
    const ocrText = document.getElementById('ocrOutput').value.trim();
    if (!ocrText) { showError('No OCR text available. Please extract text first.'); return; }
    document.getElementById('equationInput').value = ocrText;
    showSuccess('OCR text copied to equation input.');
}

// ═══════════════════════════════════════════════════════════════════
// EQUATION CLASSIFICATION
// ═══════════════════════════════════════════════════════════════════

async function identifyEquation() {
    const expression = getEquationInput();
    if (!expression) return;
    showLoading('Analysing equation...');
    clearResults();
    try {
        const response = await fetch(API_BASE + '/equation/classify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ expression })
        });
        const data = await response.json();
        if (data.success) {
            lastClassification = data;
            displayClassification(data);
            showSuccess('Equation identified as: ' + data.equationType);
        } else {
            showError('Classification failed: ' + (data.message || 'Unknown error'));
        }
    } catch (err) {
        showError('Network error: ' + err.message);
    } finally {
        hideLoading();
    }
}


function displayClassification(data) {
    document.getElementById('classificationPlaceholder').classList.add('d-none');
    document.getElementById('classificationResult').classList.remove('d-none');
    document.getElementById('eqType').textContent = data.equationType || '—';
    document.getElementById('eqDegree').textContent = data.degree !== undefined ? data.degree : '—';
    document.getElementById('eqVars').textContent = data.variables !== undefined ? data.variables : '—';
    document.getElementById('eqMethod').textContent = data.solvingMethod || '—';
    document.getElementById('eqDescription').textContent = data.description || '—';
    const solvableEl = document.getElementById('eqSolvable');
    if (data.solvable) {
        solvableEl.textContent = '✓ Yes';
        solvableEl.className = 'info-value fw-bold text-success';
        document.getElementById('solveButtonContainer').classList.remove('d-none');
        document.getElementById('unsolvableNotice').classList.add('d-none');
    } else {
        solvableEl.textContent = '✗ No';
        solvableEl.className = 'info-value fw-bold text-danger';
        document.getElementById('solveButtonContainer').classList.add('d-none');
        document.getElementById('unsolvableNotice').classList.remove('d-none');
    }
}

// ═══════════════════════════════════════════════════════════════════
// EQUATION SOLVING
// ═══════════════════════════════════════════════════════════════════

async function solveEquation() {
    const expression = getEquationInput();
    if (!expression) return;
    showLoading('Solving equation step by step...');
    try {
        const response = await fetch(API_BASE + '/equation/solve', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ expression })
        });
        const data = await response.json();
        if (data.success) {
            if (data.solved) {
                displaySolution(data);
                showSuccess('Equation solved successfully!');
            } else {
                displayUnsupported(data);
            }
        } else {
            showError('Solving failed: ' + (data.message || 'Unknown error'));
        }
    } catch (err) {
        showError('Network error: ' + err.message);
    } finally {
        hideLoading();
    }
}


function displaySolution(data) {
    document.getElementById('solutionPlaceholder').classList.add('d-none');
    document.getElementById('unsupportedResult').classList.add('d-none');
    document.getElementById('solutionResult').classList.remove('d-none');
    const badge = document.getElementById('solutionBadge');
    badge.textContent = data.equationType || '';
    badge.classList.remove('d-none');
    document.getElementById('finalAnswer').textContent = data.finalAnswer || '—';
    document.getElementById('originalEq').textContent = data.originalExpression || '—';
    const stepsContainer = document.getElementById('stepsContainer');
    stepsContainer.innerHTML = '';
    if (data.steps && data.steps.length > 0) {
        data.steps.forEach(step => {
            const el = document.createElement('div');
            el.className = 'step-item';
            el.textContent = step;
            stepsContainer.appendChild(el);
        });
        stepsContainer.scrollTop = 0;
    } else {
        stepsContainer.innerHTML = '<div class="text-muted small text-center p-3">No steps available.</div>';
    }
}

function displayUnsupported(data) {
    document.getElementById('solutionPlaceholder').classList.add('d-none');
    document.getElementById('solutionResult').classList.add('d-none');
    document.getElementById('unsupportedResult').classList.remove('d-none');
    document.getElementById('unsupportedMessage').textContent =
        data.message || 'Equation recognized but solving is not supported in the current version.';
}

// ═══════════════════════════════════════════════════════════════════
// UTILITIES
// ═══════════════════════════════════════════════════════════════════

function getEquationInput() {
    const input = document.getElementById('equationInput').value.trim();
    if (!input) { showError('Please enter an equation first.'); return null; }
    return input;
}

function setExample(expr) {
    document.getElementById('equationInput').value = expr;
    clearResults(); hideAlerts();
}

function clearAll() {
    document.getElementById('equationInput').value = '';
    document.getElementById('ocrOutput').value = '';
    document.getElementById('imageUpload').value = '';
    selectedFile = null; lastClassification = null;
    document.getElementById('dropZone').classList.remove('has-file', 'drag-over');
    document.getElementById('fileName').classList.add('d-none');
    clearResults(); hideAlerts();
}

function clearResults() {
    document.getElementById('classificationPlaceholder').classList.remove('d-none');
    document.getElementById('classificationResult').classList.add('d-none');
    document.getElementById('solutionPlaceholder').classList.remove('d-none');
    document.getElementById('solutionResult').classList.add('d-none');
    document.getElementById('unsupportedResult').classList.add('d-none');
    document.getElementById('solutionBadge').classList.add('d-none');
}

function showLoading(text) {
    const spinner = document.getElementById('loadingSpinner');
    if (!spinner) return;
    spinner.classList.remove('d-none');
    spinner.style.display = 'flex';
    const spinnerText = document.getElementById('spinnerText');
    if (spinnerText) spinnerText.textContent = text || 'Processing...';
}

function hideLoading() {
    const spinner = document.getElementById('loadingSpinner');
    if (!spinner) return;
    spinner.classList.add('d-none');
    spinner.style.display = '';
}


function showError(msg) {
    const el = document.getElementById('errorAlert');
    document.getElementById('errorMessage').textContent = msg;
    el.classList.remove('d-none');
    setTimeout(() => el.classList.add('d-none'), 6000);
}

function showSuccess(msg) {
    const el = document.getElementById('successAlert');
    document.getElementById('successMessage').textContent = msg;
    el.classList.remove('d-none');
    setTimeout(() => el.classList.add('d-none'), 4000);
}

function hideAlerts() {
    document.getElementById('errorAlert').classList.add('d-none');
    document.getElementById('successAlert').classList.add('d-none');
}

function copySteps(buttonEl) {
    const steps = document.querySelectorAll('.step-item');
    if (!steps.length) return;
    const text = Array.from(steps).map(s => s.textContent).join('\n');
    const finalAnswer = document.getElementById('finalAnswer').textContent;
    const fullText = 'SOLUTION\n' + '='.repeat(40) + '\nFinal Answer: ' + finalAnswer + '\n\nStep-by-Step:\n' + text;
    navigator.clipboard.writeText(fullText).then(() => {
        const btn = buttonEl || document.activeElement?.closest('button');
        if (btn) {
            btn.classList.add('btn-copied');
            btn.innerHTML = '<i class="bi bi-check me-1"></i>Copied!';
            setTimeout(() => { btn.classList.remove('btn-copied'); btn.innerHTML = '<i class="bi bi-clipboard me-1"></i>Copy'; }, 2000);
        }
    }).catch(() => showError('Could not copy to clipboard.'));
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}