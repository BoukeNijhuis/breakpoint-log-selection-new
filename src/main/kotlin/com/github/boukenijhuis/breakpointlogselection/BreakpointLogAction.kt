package com.github.boukenijhuis.breakpointlogselection

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
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise


class BreakpointLogAction : AnAction() {

    // exposed for testing (will be updated later)
    var breakpoint: Promise<XLineBreakpoint<*>?> = resolvedPromise()
        private set

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(event: AnActionEvent) {
        // get current project and file
        val project = event.project ?: return
        val currentFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        // determine the position variables
        val offset = editor.caretModel.offset
        val currentPosition = XSourcePositionImpl.createByOffset(currentFile, offset) as XSourcePosition
        val nextLinePosition = XSourcePositionImpl.create(currentFile, currentPosition.line + 1)

        // check the selection and determine the breakpoint position
        val selectedText = editor.selectionModel.selectedText
        var position = currentPosition
        if (selectedText != null) {
            position = nextLinePosition
        }

        // increase the line number if the determined position does not support a breakpoint
        while (!getInstance().canPutBreakpointAt(project, currentFile, position.line)) {
            position = XSourcePositionImpl.create(currentFile, position.line + 1)

            // when we move past the last line
            if (position.line + 1 > editor.document.lineCount) {
                return
            }
        }

        // always toggle (even if there is no selection)
        breakpoint =
            XBreakpointUtil.toggleLineBreakpoint(project, position, true, editor, false, false, true)

        // update the breakpoint with the log expression
        if (selectedText != null && breakpoint is AsyncPromise) {
            breakpoint.then {
                it?.suspendPolicy = SuspendPolicy.NONE
                it?.setLogExpression("\"$selectedText = [\" + $selectedText + \"]\"")
            }
        }
    }
}