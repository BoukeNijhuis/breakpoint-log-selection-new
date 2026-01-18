package com.github.boukenijhuis.breakpointlogselection

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.SuspendPolicy

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

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    companion object {
        fun testBreakpointCreation(myPluginTest: MyPluginTest, fileName: String) {
            myPluginTest.myFixture.configureByFiles(fileName)
            val manager = XDebuggerManager.getInstance(myPluginTest.project).breakpointManager;

            val breakpointsBefore = manager.allBreakpoints.size

            val action = BreakpointLogAction()
            val actionEvent = createTestEvent(action)
            action.actionPerformed(actionEvent)
            PlatformTestUtil.waitForPromise(action.breakpoint)

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
            action.actionPerformed(actionEvent)
            PlatformTestUtil.waitForPromise(action.breakpoint)

            // there should be no breakpoint extra
            val breakpointsAfterDouble = manager.allBreakpoints.size
            assertEquals("Expected exactly zero created breakpoints.", 0, breakpointsAfterDouble - breakpointsBefore)
        }

        fun testNoBreakpointCreation(myPluginTest: MyPluginTest, fileName: String) {
            myPluginTest.myFixture.configureByFiles(fileName)
            val manager = XDebuggerManager.getInstance(myPluginTest.project).getBreakpointManager();

            val breakpointsBefore = manager.getAllBreakpoints().size

            val actionEvent = createTestEvent(BreakpointLogAction())
            val action = BreakpointLogAction()
            action.actionPerformed(actionEvent)
            PlatformTestUtil.waitForPromise(action.breakpoint)

            val breakpointsAfter = manager.getAllBreakpoints().size

            // there should be one breakpoint extra
            assertEquals("Expected exactly zero created breakpoint.", 0, breakpointsAfter - breakpointsBefore)
        }
    }
}

