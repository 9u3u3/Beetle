const term = new window.Terminal({
  cursorBlink: true,
  fontSize: 14,
  fontFamily: "monospace",
});

term.open(document.getElementById("terminal"));

const fitAddon = new FitAddon.FitAddon();
term.loadAddon(fitAddon);

let currentCommand = '';

setTimeout(() => {
  const textarea = term.textarea;
  if (textarea) {
    textarea.setAttribute("inputmode", "none");
    textarea.focus();
    term.focus();
  }
}, 0);

term.onData((data) => {
  const code = data.charCodeAt(0);

  if (code < 0x20 && data !== '\r' && data !== '\t') {
    console.log('Ignoring control character:', code);
    return;
  }

  if (data === '\r') {
    console.log('COMMAND ENTERED via onData:', currentCommand);
    term.write('\r\n');
    AndroidTerminal.sendInput(currentCommand);
    currentCommand = '';
  } else if (data === '\x7f') {
    if (currentCommand.length > 0) {
      currentCommand = currentCommand.slice(0, -1);
      term.write('\b \b');
    }
  } else {
    currentCommand += data;
    term.write(data);
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
  if (!fireOnData(text)) {
    term.write(text);
    currentCommand += text;
  }
};

window.performBackspace = function () {
  term.focus();
  if (!fireOnData('\x7f')) {
    if (currentCommand.length > 0) {
      currentCommand = currentCommand.slice(0, -1);
    }
    term.write("\b \b");
  }
};

window.performEnter = function () {
  term.focus();
  if (!fireOnData('\r')) {
    term.write("\r\n");
    AndroidTerminal.sendInput(currentCommand);
    currentCommand = '';
  }
};

window.getCurrentCommand = function() {
  return currentCommand;
};

window.term = term;

window.fitTerminal = function() {
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
        AndroidTerminalResize.resizeTerminal(dims.rows + 1, dims.cols + 2);
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