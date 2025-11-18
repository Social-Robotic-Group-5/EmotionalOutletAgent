package furhatos.app.outletagentskill.flow.main

import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.behavior
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.onNoResponse
import furhatos.flow.kotlin.onResponse
import furhatos.flow.kotlin.state
import furhatos.gestures.Gestures
import furhatos.nlu.Intent
import furhatos.nlu.common.No
import furhatos.nlu.common.Yes
import furhatos.util.Language
import furhatos.flow.kotlin.voice.Voice

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
        furhat.gesture(Gestures.Smile, async = true)
        furhat.ask("How are you feeling today?")
    }

    onReentry{
        furhat.ask({
            +"Okay!"
            +behavior {
                furhat.gesture(Gestures.Smile)
            }
            +"What do you want to share?"
        })
    }

    //The volume setting only works for Amazon Polly voices
    onResponse<NegativeFeeling> {
        val originalVoice = furhat.voice
        val sadVoice = Voice(
            gender = originalVoice.gender,
            language = originalVoice.language,
            pitch = "medium",
            rate = 0.90,
            volume = "soft"
        )

        furhat.voice = sadVoice
        furhat.say {
            +behavior { furhat.gesture(Gestures.ExpressSad, async = true) }
            +"I am ${furhat.voice.emphasis("so")} sorry to hear that"
        }

        furhat.ask {
            +"Could you tell me what made you feel  ${furhat.voice.emphasis("sad")} today?"
        }

        furhat.voice = originalVoice
    }

    onResponse<PositiveFeeling> {
        val originalVoice = furhat.voice
        val happyVoice = Voice(
            gender = originalVoice.gender,
            language = originalVoice.language,
            pitch = "high",
            rate = 1.1,
            volume = "medium"
        )

        furhat.voice = happyVoice
        furhat.ask {
            +behavior { furhat.gesture(Gestures.BigSmile, async = true) }
            +"I'm ${furhat.voice.emphasis("glad")} to hear that. Could you tell me what made you feel ${
                furhat.voice.emphasis(
                    "happy"
                )
            } today?"
        }
    }


    onResponse {
        val confirm = furhat.askYN("Thank you for sharing that with me. Is there anything else you wish to share?")

        if(confirm) {
            reentry()
        } else {
            goto(EndConversation)
        }
    }

    onNoResponse { // Catches silence
        furhat.say("I didn't hear anything")
        val confirmExit = furhat.askYN("Do you want to stop the session instead?")

        if(confirmExit) {
            goto(EndConversation)
        } else {
            reentry()
        }
    }
}
