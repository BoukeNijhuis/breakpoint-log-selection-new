# breakpoint-log-selection

![Build](https://github.com/BoukeNijhuis/breakpoint-log-selection-new/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
Breakpoints can be used for temporary logging. This kind of logging will never accidentally be pushed to your VCS or to production, because a breakpoint is not code. Therefore, you cannot push it.

A breakpoint is a logging breakpoint when it fulfills the following criteria:
- it is non suspending
- it has a log expression

If both conditions are true we are talking about a logging breakpoint. These are fantastic tools to use, but there is a downside: you can only create one by doing multiple clicks with the mouse. With this plugin you can do the same but with a keyboard shortcut.

It works as follows:
1. select the variable or expression that you would like to log
2. press the assigned shortcut (default: ctrl-command-F8)

This will create a non suspending breakpoint that will log the selected variable or expression on the next line that support breakpoints. The main reason for the next line is to prevent the logging of empty variables.

The default shortcut can be replaced by assigning a new shortcut to the action 'Log Selection with Breakpoint'.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "breakpoint-log-selection"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Download the [latest release](https://github.com/BoukeNijhuis/breakpoint-log-selection/releases-new/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation

