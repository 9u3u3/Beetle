// Initializes the terminal
const term = new window.Terminal({
  cursorBlink: true,
  fontSize: 14,
  fontFamily: "monospace",
});

term.open(document.getElementById("terminal"));
let currentCommand = '';

// Disable the virtual keyboard
setTimeout(() => {
  const textarea = term.textarea;
  if (textarea) {
    textarea.setAttribute("inputmode", "none"); // Disable the IME (Input Method Editor)
    textarea.focus();
    term.focus();
  }
}, 0);

// Called When a command is entered
// This onData is required as it has features like going through previous commands
// So this onData thing is required
term.onData((data) => {
  if (data === '\r') { // If Enter key is pressed
    console.log('COMMAND ENTERED via onData:', currentCommand);
    term.write('\r\n');
    AndroidTerminal.sendInput(currentCommand)
    currentCommand = '';
  } else if (data === '\x7f') { // If Backspace key is pressed
    if (currentCommand.length > 0) {
      currentCommand = currentCommand.slice(0, -1);
      term.write('\b \b');
    }
  } else { // If any other normal key is pressed
    currentCommand += data;
    term.write(data);
  }
});

// This function triggers xterm.js's internal input events
// It accesses xterm's private API to fire onData events properly
// Basically this function triggers onData of xtermjs
function fireOnData(data) {
  // Access xterm's internal core object and fire the onData event
  // term._core._onData is xterm's internal event emitter for input data
  if (term._core && term._core._onData && typeof term._core._onData.fire === 'function') {
    term._core._onData.fire(data);  // Fire the internal event
    return true;  // Success
  }
  return false;  // Failed - API might have changed
}

// Handle text insertion from custom keyboard
window.insertText = function (text) {

  term.focus();
  let success = true;
  if (!fireOnData(text)) success = false;

  // Fallback in case internal API fails (shouldn't happen with current setup)
  if (!success) {
    console.log('Using fallback for insertText');
    term.write(text);           // Just display the text
    currentCommand += text;     // Manually track command
    console.log('Current command (fallback):', currentCommand);
  }

};

// Handle backspace from custom keyboard
window.performBackspace = function () {
  term.focus();
  // Send backspace character (\x7f = ASCII DEL) through proper channels
  if (!fireOnData('\x7f')) {
    // Fallback manual handling
    console.log('Using fallback for backspace');
    if (currentCommand.length > 0) {
      currentCommand = currentCommand.slice(0, -1);
      console.log('After backspace (fallback):', currentCommand);
    }
    term.write("\b \b");
  }
};

// Handle enter key from custom keyboard
window.performEnter = function () {
  term.focus();
  // Send carriage return (\r) through proper channels
  if (!fireOnData('\r')) {
    // Fallback manual handling
    console.log('Using fallback for enter');
    term.write("\r\n");
    console.log('COMMAND ENTERED (fallback):', currentCommand);
    currentCommand = '';
  }
};

// Utility function to get current command (for debugging)
window.getCurrentCommand = function() {
  return currentCommand;
};

// Expose terminal object globally for debugging
window.term = term;