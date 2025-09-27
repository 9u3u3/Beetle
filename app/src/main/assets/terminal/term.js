// === terminal.js ===

// Initialize terminal
const term = new window.Terminal({
  cursorBlink: true,
  fontSize: 14,
  fontFamily: "monospace",
});
term.open(document.getElementById("terminal"));

// Disable Android IME popup
setTimeout(() => {
  const textarea = term.textarea;
  if (textarea) {
    textarea.setAttribute("inputmode", "none");   // disables keyboard
    textarea.setAttribute("readonly", "true");   // blocks input

    textarea.focus()
    term.focus()

    const clickEvent = new MouseEvent("mousedown", { bubbles: true, cancelable: true });
    textarea.dispatchEvent(clickEvent);
    const upEvent = new MouseEvent("mouseup", { bubbles: true, cancelable: true });
    textarea.dispatchEvent(upEvent);
    const focusEvent = new FocusEvent("focus", { bubbles: true, cancelable: true });
    textarea.dispatchEvent(focusEvent);

  }
}, 0);

// === Exposed functions for Android WebView ===

// Insert text at cursor
window.insertText = function (text) {
  term.focus();
  term.write(text);
};

// Perform backspace
window.performBackspace = function () {
  term.focus();
  // send DEL (works with PTY), fallback is \b \b for local erase
  if (term.send) {
    term.send("\x7f"); // ASCII DEL
  } else {
    term.write("\b \b");
  }
};

// Perform enter
window.performEnter = function () {
  term.focus();
  if (term.send) {
    term.send("\r"); // send enter to backend
  } else {
    term.write("\r\n"); // local newline
  }
};

// Optional: expose the terminal object itself (for debugging)
window.term = term;