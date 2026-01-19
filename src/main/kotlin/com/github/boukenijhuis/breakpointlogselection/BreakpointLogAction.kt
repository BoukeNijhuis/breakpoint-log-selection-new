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

        // decide where to put it: next line if something is selected, otherwise current line
        val selectedText = editor.selectionModel.selectedText?.trim()
        var targetLine = if (selectedText != null) currentPosition.line + 1 else currentPosition.line

        // find the first valid line for a breakpoint starting from targetLine
        val lineCount = editor.document.lineCount
        while (targetLine < lineCount &&
            !getInstance().canPutBreakpointAt(project, currentFile, targetLine)) {
            targetLine++
        }

        // do not exceed the end of the document
        if (targetLine >= lineCount) return

        val position = XSourcePositionImpl.create(currentFile, targetLine)


        // always toggle (even if there is no selection)
        breakpoint =
            XBreakpointUtil.toggleLineBreakpoint(project, position, true, editor, false, false, true)

        // update the breakpoint with the log expression
        // todo: check for multiple lines
        if (selectedText != null && breakpoint is AsyncPromise) {
            breakpoint.then {
                it?.suspendPolicy = SuspendPolicy.NONE
                it?.setLogExpression("\"$selectedText = [$selectedText]\"")
            }
        }
    }
}