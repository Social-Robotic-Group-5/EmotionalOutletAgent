package furhatos.app.outletagentskill.flow.main

import furhatos.app.outletagentskill.flow.ai.addToHistory
import furhatos.app.outletagentskill.flow.ai.geminiClient
import furhatos.app.outletagentskill.flow.ai.getConversationHistory
import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.onNoResponse
import furhatos.flow.kotlin.onResponse
import furhatos.flow.kotlin.state
import furhatos.flow.kotlin.users
import furhatos.gestures.Gestures

val InteractWithIntervention: State = state {
    onEntry {
        goto(PositiveQuestioning)
    }
}

val PositiveQuestioning: State = state {
    onEntry {
        furhat.gesture(Gestures.Smile, async = true)
        furhat.ask("What makes you feeling good today?")
    }

    onResponse {
        users.current.history = it.text
        goto(FollowUp)
    }
}

val FollowUp: State = state {
    onEntry {
        val llmAnswer = call {
            geminiClient.getCompletion(
                users.current.history,
                getConversationHistory().dropLast(1),
                systemPrompt = "Follow up this: "
            )
        } as String?

        if (llmAnswer != null) {
            furhat.gesture(Gestures.Smile, async = true)
            addToHistory("model", llmAnswer)
            furhat.ask(llmAnswer)
        } else {
            val confirmRetry = furhat.askYN(geminiFailResponse)
            if(confirmRetry) {
                furhat.say("Okay. I will retry thinking")
                reentry()
            } else {
                furhat.say("Okay. I will end this session")
                goto(EndConversation)
            }
        }
    }

    onResponse {
        users.current.history = it.text
    }

    onNoResponse {
        furhat.say(defaultSilenceResponse)
    }
}


val Reappraisal: State = state {
    onEntry {
        furhat.gesture(Gestures.Smile, async = true)
        furhat.ask("What makes you feeling good today?")
    }
}

val FinalFollowUp: State = state {
    onEntry {
        furhat.gesture(Gestures.Smile, async = true)
        furhat.ask("What makes you feeling good today?")
    }

    onResponse {
        goto(EndConversation)
    }
}