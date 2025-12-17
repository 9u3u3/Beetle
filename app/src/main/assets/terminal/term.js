const term = new window.Terminal({
  cursorBlink: true,
  fontSize: 11,
  fontFamily: "monospace",
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
  console.log('CTRL state set to:', isCtrlOn);
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
      console.log('Sending Ctrl+' + char + ' (0x' + ctrlCode.toString(16) + ')');
      AndroidTerminal.sendRaw(ctrlChar);

      // Turn off CTRL and notify Android
      isCtrlOn = false;
      if (typeof AndroidTerminalCtrl !== 'undefined') {
        AndroidTerminalCtrl.ctrlUsed();
      }
      return; // Don't send the original character
    }
  }

  // Pass all keyboard input directly to the PTY
  // The PTY and shell will handle all processing (echo, line editing, etc.)
  // This ensures interactive CLI tools (like npm create) work correctly

  // Log for debugging (can be removed later)
  if (data.length === 1) {
    const code = data.charCodeAt(0);
    if (code === 0x0D) {
      console.log('Enter key pressed');
    } else if (code === 0x1B) {
      console.log('ESC key pressed');
    } else if (code < 0x20) {
      console.log('Control char:', code.toString(16));
    }
  } else if (data.charCodeAt(0) === 0x1B) {
    console.log('Escape sequence:', data.split('').map(c => c.charCodeAt(0).toString(16)).join(' '));
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

window.insertText = function (text) {
  term.focus();
  // Send each character to PTY, let it echo
  for (let i = 0; i < text.length; i++) {
    const char = text[i];
    AndroidTerminal.sendRaw(char);
    currentCommand += char;
  }
};

window.performBackspace = function () {
  term.focus();
  // Send backspace to PTY
  AndroidTerminal.sendRaw('\x7f');
  if (currentCommand.length > 0) {
    currentCommand = currentCommand.slice(0, -1);
  }
};

window.performEnter = function () {
  term.focus();
  // Send carriage return to PTY
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

window.performPaste = async function () {
  term.focus();
  try {
    const text = await navigator.clipboard.readText();
    // Send each character to PTY
    for (let i = 0; i < text.length; i++) {
      const char = text[i];
      AndroidTerminal.sendRaw(char);
      currentCommand += char;
    }
  } catch (e) {
    console.error("Paste failed:", e);
  }
};

window.performCut = function () {
  const selection = term.getSelection();
  if (selection) {
    navigator.clipboard.writeText(selection);
    // Terminal doesn't support cutting selected text easily
    // So just copy it
  }
};

window.performSelectAll = function () {
  term.selectAll();
};

window.performFind = function () {
  // xterm.js doesn't have built-in search UI
  console.log("Find not supported in terminal");
};

window.performCtrlC = function () {
  term.focus();
  // Send Ctrl+C to PTY
  AndroidTerminal.sendRaw('\x03');
  currentCommand = '';
};

window.performCtrlD = function () {
  term.focus();
  // Send EOF (Ctrl+D) to PTY
  AndroidTerminal.sendRaw('\x04');
  currentCommand = '';
};

window.performEscape = function () {
  term.focus();
  // Send ESC to PTY
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
    // Retry on error
    setTimeout(window.fitTerminal, 200);
  }
};

setTimeout(window.fitTerminal, 100);
setTimeout(window.fitTerminal, 300);
setTimeout(window.fitTerminal, 500);
setTimeout(window.fitTerminal, 1000);
setTimeout(window.fitTerminal, 2000);
setTimeout(window.fitTerminal, 3000);

window.addEventListener('resize', () => {
  setTimeout(window.fitTerminal, 100);
});

// Runtime font size change
window.setFontSize = function (size) {
  term.options.fontSize = size;
  console.log('[Terminal] Font size set to:', size);
  // Re-fit terminal to recalculate rows/cols with new font size
  setTimeout(() => {
    window.fitTerminal();
  }, 50);
};
