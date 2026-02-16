const term = new window.Terminal({
  cursorBlink: true,
  fontSize: 11,
  fontFamily: "monospace",
  scrollback: 10000, // Large scrollback buffer like Termux
});

term.open(document.getElementById("terminal"));

const fitAddon = new FitAddon.FitAddon();
term.loadAddon(fitAddon);
const dims = fitAddon.proposeDimensions();
AndroidTerminalResize.getRowsCols(dims.rows, dims.cols);

let currentCommand = '';

// CTRL modifier state for portrait keyboard
let isCtrlOn = false;

// Function to set CTRL state from Android
window.setCtrlState = function (state) {
  isCtrlOn = state;
};

// Function to get current CTRL state
window.getCtrlState = function () {
  return isCtrlOn;
};

setTimeout(() => {
  const textarea = term.textarea;
  if (textarea) {
    const updateInputMode = () => {
      if (window.matchMedia("(orientation: landscape)").matches) {
        textarea.setAttribute("inputmode", "none");
      } else {
        textarea.setAttribute("inputmode", "text");
      }
    };

    window.addEventListener("resize", updateInputMode);
    updateInputMode(); // Initial check

    textarea.focus();
    term.focus();
  }
}, 0);

term.onData((data) => {
  // Check if CTRL is on and this is a single printable character
  if (isCtrlOn && data.length === 1) {
    const char = data.toUpperCase();
    const code = char.charCodeAt(0);

    // Convert A-Z to Ctrl+A - Ctrl+Z (ASCII 1-26)
    if (code >= 65 && code <= 90) {
      const ctrlCode = code - 64; // A=1, B=2, ... Z=26
      const ctrlChar = String.fromCharCode(ctrlCode);
      AndroidTerminal.sendRaw(ctrlChar);

      // Turn off CTRL and notify Android
      isCtrlOn = false;
      if (typeof AndroidTerminalCtrl !== 'undefined') {
        AndroidTerminalCtrl.ctrlUsed();
      }
      return; // Don't send the original character
    }
  }

  // Send everything directly to PTY
  AndroidTerminal.sendRaw(data);

  // Track command for local purposes (optional)
  if (data === '\r') {
    currentCommand = '';
  } else if (data === '\x7f') {
    if (currentCommand.length > 0) {
      currentCommand = currentCommand.slice(0, -1);
    }
  } else if (data.length === 1 && data.charCodeAt(0) >= 0x20) {
    currentCommand += data;
  }
});

function fireOnData(data) {
  if (term._core && term._core._onData && typeof term._core._onData.fire === 'function') {
    term._core._onData.fire(data);
    return true;
  }
  return false;
}

// Send entire string at once instead of char-by-char
window.insertText = function (text) {
  term.focus();
  AndroidTerminal.sendRaw(text);
  currentCommand += text;
};

window.performBackspace = function () {
  term.focus();
  AndroidTerminal.sendRaw('\x7f');
  if (currentCommand.length > 0) {
    currentCommand = currentCommand.slice(0, -1);
  }
};

window.performEnter = function () {
  term.focus();
  AndroidTerminal.sendRaw('\r');
  currentCommand = '';
};

window.performCopy = function () {
  const selection = term.getSelection();
  if (selection) {
    navigator.clipboard.writeText(selection)
      .catch(e => console.error("Copy failed:", e));
  }
};

// Send entire paste text at once instead of char-by-char
window.performPaste = async function () {
  term.focus();
  try {
    const text = await navigator.clipboard.readText();
    if (text) {
      AndroidTerminal.sendRaw(text);
      currentCommand += text;
    }
  } catch (e) {
    console.error("Paste failed:", e);
  }
};

window.performCut = function () {
  const selection = term.getSelection();
  if (selection) {
    navigator.clipboard.writeText(selection);
  }
};

window.performSelectAll = function () {
  term.selectAll();
};

window.performFind = function () {
  // xterm.js doesn't have built-in search UI
};

window.performCtrlC = function () {
  term.focus();
  AndroidTerminal.sendRaw('\x03');
  currentCommand = '';
};

window.performCtrlD = function () {
  term.focus();
  AndroidTerminal.sendRaw('\x04');
  currentCommand = '';
};

window.performEscape = function () {
  term.focus();
  AndroidTerminal.sendRaw('\x1B');
  currentCommand = '';
};

window.getCurrentCommand = function () {
  return currentCommand;
};

window.term = term;

window.fitTerminal = function () {
  try {
    const container = document.getElementById('terminal');
    const rect = container.getBoundingClientRect();

    if (rect.width === 0 || rect.height === 0) {
      setTimeout(window.fitTerminal, 200);
      return;
    }

    fitAddon.fit();

    const dims = fitAddon.proposeDimensions();
    if (dims) {
      if (window.AndroidTerminalResize) {
        AndroidTerminalResize.getRowsCols(dims.rows, dims.cols);
      } else {
        console.warn('AndroidTerminalResize not available');
      }
    } else {
      console.warn('FitAddon returned null dimensions');
    }
  } catch (e) {
    console.error('Error in fitTerminal:', e);
    setTimeout(window.fitTerminal, 200);
  }
};

// Use ResizeObserver for reliable fit instead of hardcoded timeout cascade
const terminalContainer = document.getElementById('terminal');
if (typeof ResizeObserver !== 'undefined' && terminalContainer) {
  const resizeObserver = new ResizeObserver(() => {
    window.fitTerminal();
  });
  resizeObserver.observe(terminalContainer);
}

// One initial call as fallback
setTimeout(window.fitTerminal, 100);

window.addEventListener('resize', () => {
  setTimeout(window.fitTerminal, 100);
});

// Runtime font size change
window.setFontSize = function (size) {
  term.options.fontSize = size;
  setTimeout(() => {
    window.fitTerminal();
  }, 50);
};

// Bottom padding for portrait mode IME keyboard
window.setBottomPadding = function (paddingPx) {
  const terminalEl = document.getElementById('terminal');
  if (terminalEl) {
    terminalEl.style.paddingBottom = paddingPx + 'px';
    if (fitAddon) {
      setTimeout(() => {
        fitAddon.fit();
        const dims = fitAddon.proposeDimensions();
        if (dims && window.AndroidTerminalResize) {
          AndroidTerminalResize.resizeTerminal(dims.rows, dims.cols);
        }
      }, 50);
    }
  }
};

// Add extra scroll space at bottom - allows scrolling content above keyboard
window.setExtraScrollSpace = function (amount) {
  const existingStyle = document.getElementById('extra-scroll-style');
  if (existingStyle) {
    existingStyle.remove();
  }

  if (amount) {
    const style = document.createElement('style');
    style.id = 'extra-scroll-style';
    style.textContent = `
      .xterm-viewport {
        padding-bottom: ${amount} !important;
      }
      .xterm-screen {
        padding-bottom: ${amount} !important;
      }
    `;
    document.head.appendChild(style);
  }
};

// Auto-enable extra scroll space in portrait mode
const checkOrientation = () => {
  if (window.matchMedia("(orientation: portrait)").matches) {
    window.setExtraScrollSpace('50vh');
  } else {
    window.setExtraScrollSpace(null);
  }
};

setTimeout(checkOrientation, 100);
window.addEventListener('resize', checkOrientation);
