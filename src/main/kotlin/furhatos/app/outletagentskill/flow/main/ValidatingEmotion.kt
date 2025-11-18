package furhatos.app.outletagentskill.flow.main

import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.behavior
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.onResponse
import furhatos.flow.kotlin.state
import furhatos.gestures.Gestures
import furhatos.nlu.Intent
import furhatos.nlu.common.No
import furhatos.nlu.common.Yes
import furhatos.util.Language

class PositiveFeeling : Intent() {
    override fun getExamples(lang: Language) = listOf(
        "I'm good",
        "good",
        "I'm fine",
        "fine",
        "I'm well",
        "well",
        "pretty good",
        "I'm happy",
        "happy"
    )
}

class NegativeFeeling : Intent() {
    override fun getExamples(lang: Language) = listOf(
        "I'm sad",
        "sad",
        "bad",
        "not so good",
        "not good",
        "I'm feeling down",
        "I'm upset"
    )
}

val ValidatingEmotion: State = state{


    onEntry {
        furhat.ask("How are you feeling today?")
    }


    onResponse<NegativeFeeling> {
        furhat.ask({
            +"I'm sorry to hear that"
            +behavior {
                furhat.gesture(Gestures.ExpressSad)
            }
            +"Could you tell me what made you feel sad today?"
        })
    }


    onResponse<PositiveFeeling> {
        furhat.ask("I'm glad to hear that. Could you tell me what made you feel happy today?")
    }


    onResponse {
        furhat.say("Thank you for sharing that with me. Is there anything else you wish to share?")
    }

    onResponse<Yes> {
        reentry()
    }

    onResponse<No> {
        goto(EndConversation)
    }
}

