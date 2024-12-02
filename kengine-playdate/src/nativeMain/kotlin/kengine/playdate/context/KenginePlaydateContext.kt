package kengine.playdate.context

import com.kengine.context.Context

class KenginePlaydateContext private constructor() : Context() {

    companion object {
        private var currentContext: KenginePlaydateContext? = null

        fun get(): KenginePlaydateContext {
            if (currentContext == null) {
                currentContext = KenginePlaydateContext()
            }
            return currentContext ?: throw IllegalStateException("Failed to create kengine playdate context")
        }

    }

    override fun cleanup() { }

}

