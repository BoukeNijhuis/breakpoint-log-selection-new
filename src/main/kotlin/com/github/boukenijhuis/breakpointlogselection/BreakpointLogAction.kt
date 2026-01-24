package com.github.boukenijhuis.breakpointlogselection

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.xdebugger.XDebuggerUtil.getInstance
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.impl.XSourcePositionImpl
import com.intellij.xdebugger.impl.breakpoints.XBreakpointUtil
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

class BreakpointLogAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(event: AnActionEvent) {
        // get current project and file
        val project = event.project ?: return
        val currentFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val debuggerUtil = getInstance()

        // determine the position variables
        val offset = editor.caretModel.offset
        val currentPosition = debuggerUtil.createPositionByOffset(currentFile, offset) as XSourcePosition

        // decide where to put it: next line if something is selected, otherwise current line
        val selectedText = editor.selectionModel.selectedText?.trim()
        var nextValidLine = if (selectedText != null) currentPosition.line + 1 else currentPosition.line

        // find the first valid line for a breakpoint starting from targetLine
        val lineCount = editor.document.lineCount

        // small function for readability
        fun cannotAddBreakPointAtNextLine(): Boolean = nextValidLine < lineCount &&
                !debuggerUtil.canPutBreakpointAt(project, currentFile, nextValidLine)

        // find the next line which can have a breakpoint
        while (cannotAddBreakPointAtNextLine()) {
            nextValidLine++
        }

        // warn the user when exceeding the end of the file
        if (nextValidLine >= lineCount) {
            val notification = Notification(
                "Custom Notification Group",
                "Log Selection with Breakpoint Plugin",
                "Cannot put a breakpoint after the end of the file.",
                NotificationType.INFORMATION
            )
            Notifications.Bus.notify(notification)
            return
        }

        val position = XSourcePositionImpl.create(currentFile, nextValidLine)

        // always toggle (even if there is no selection)
        val breakpoint =
            XBreakpointUtil.toggleLineBreakpoint(project, position, true, editor, false, false, true)

        // update the breakpoint with the log expression
        // todo: check for multiple lines
        if (selectedText != null) {
            breakpoint.then {
                it?.suspendPolicy = SuspendPolicy.NONE
                it?.setLogExpression("\"$selectedText = [$selectedText]\"")
            }
        }
    }
}