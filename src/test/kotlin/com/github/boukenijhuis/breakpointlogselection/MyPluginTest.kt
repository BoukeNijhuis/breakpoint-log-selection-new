package com.github.boukenijhuis.breakpointlogselection

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XBreakpointManager
import java.lang.Thread.sleep

class MyPluginTest : BasePlatformTestCase() {

    fun testBreakpointCreationOnNextLine() {
        testBreakpointCreation(this, "Main.java")
    }

    fun testBreakpointCreationBeforeEmtpyLine() {
        testBreakpointCreation(this, "MainEmptyLine.java")
    }

    fun testBreakpointCreationLastLine() {
        testNoBreakpointCreation(this, "MainLastLine.java")
    }

    fun testBreakpointToggleNoSelection() {
        val manager = setup("MainNoSelection.java")

        val breakpointsBefore = manager.allBreakpoints.size
        performBreakpointAction()
        val breakpointsAfter = manager.allBreakpoints.size
        assertEquals("Expected exactly one created breakpoint.", 1, breakpointsAfter - breakpointsBefore)

        val breakpoint = manager.allBreakpoints.last()
        assertEquals(
            "Should be on line 2 (0-indexed)",
            2,
            (breakpoint as com.intellij.xdebugger.breakpoints.XLineBreakpoint<*>).line
        )
        assertNull("Should not have log expression", breakpoint.logExpressionObject)

        // Toggle off
        performBreakpointAction()
        assertEquals("Expected breakpoint to be removed.", 0, manager.allBreakpoints.size - breakpointsBefore)
    }

    fun testLogExpressionContent() {
        val manager = setup("Main.java")
        performBreakpointAction()

        val breakpoint = manager.allBreakpoints.last()
        assertEquals("\"aap = [aap]\"", breakpoint.logExpressionObject?.expression)
    }

    fun testMultiLineSelection() {
        val manager = setup("MainMultiLine.java")
        performBreakpointAction()

        val breakpoint = manager.allBreakpoints.last()
        // It currently just takes the selected text as is, including newlines if they were there.
        // However, selectedText in the action is trimmed.
        val expected = "aap = \"noot\";\n        System.out.println(\"Hello world!\");"
        assertEquals("\"$expected = [$expected]\"", breakpoint.logExpressionObject?.expression)
        // Should be on line 4 (0-indexed). The selection started on line 2 and ended on line 3.
        // The caret is at the end of selection, which is line 3 (if it's right after the last character).
        // If the selection is multi-line, currentPosition.line is where the caret is.
        // If caret is on line 3, then nextValidLine = 3 + 1 = 4.
        assertEquals(4, (breakpoint as com.intellij.xdebugger.breakpoints.XLineBreakpoint<*>).line)
    }

    private fun testBreakpointCreation(myPluginTest: MyPluginTest, fileName: String) {
        val manager = setup(fileName)

        val breakpointsBefore = manager.allBreakpoints.size
        performBreakpointAction()
        val breakpointsAfter = manager.allBreakpoints.size

        // there should be one breakpoint extra
        assertEquals("Expected exactly one created breakpoint.", 1, breakpointsAfter - breakpointsBefore)

        // check the created breakpoint
        val breakpoint = manager.allBreakpoints.last()
        assertEquals("Expected the breakpoint to not suspend.", breakpoint.suspendPolicy, SuspendPolicy.NONE)
        assertNotNull(
            "Expected the breakpoint the have a log expression",
            breakpoint.logExpressionObject?.expression
        )

        // call the plugin again to remove the breakpoint
        performBreakpointAction()

        // there should be no breakpoint extra
        val breakpointsAfterDouble = manager.allBreakpoints.size
        assertEquals("Expected exactly zero created breakpoints.", 0, breakpointsAfterDouble - breakpointsBefore)
    }

    private fun testNoBreakpointCreation(myPluginTest: MyPluginTest, fileName: String) {
        val manager = setup(fileName)

        val breakpointsBefore = manager.getAllBreakpoints().size
        performBreakpointAction()
        val breakpointsAfter = manager.getAllBreakpoints().size

        // there should be one breakpoint extra
        assertEquals("Expected exactly zero created breakpoint.", 0, breakpointsAfter - breakpointsBefore)
    }

    /**
     * Configures the test fixture with the specified source files and returns the
     * breakpoint manager for the current project.
     *
     * @param filePaths The paths of the files to load into the fixture.
     * @return The [XBreakpointManager] instance for the current project.
     */
    private fun setup(vararg filePaths: String): XBreakpointManager {
        myFixture.configureByFiles(*filePaths)
        val manager = XDebuggerManager.getInstance(project).breakpointManager
        return manager
    }

    /**
     * Executes the `BreakpointLogAction` within the test environment.
     *
     * The method constructs a synthetic action event and triggers the action,
     * which toggles a breakpoint at the current caret position. After performing
     * the action, it processes any pending UI events for a short period to allow
     * asynchronous breakpoint creation or removal to complete.
     *
     * This helper is intended for use in unit tests that verify breakpoint
     * behaviour, ensuring that the action has been fully executed before any
     * assertions are made on the breakpoint state.
     */
    private fun performBreakpointAction() {
        val action = BreakpointLogAction()
        val actionEvent = createTestEvent(action)
        ActionUtil.performAction(action, actionEvent)

        // necessary to wait for the breakpoint promise to resolve
        (0..10).forEach {
            UIUtil.dispatchAllInvocationEvents()
            sleep(5)
        }
    }

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }
}

