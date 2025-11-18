package furhatos.app.outletagentskill.flow.main

import furhatos.flow.kotlin.State
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.onResponse
import furhatos.flow.kotlin.state
import furhatos.flow.kotlin.users
import furhatos.nlu.Intent
import furhatos.nlu.common.No
import furhatos.nlu.common.Yes
import furhatos.util.Language

class GreetReply : Intent() {
    override fun getExamples(lang: Language) = listOf(
        "Hi",
        "Hello",
        "Howdy",
        "Nice to meet you",
        "Likewise"
    )
}

val StartConversation: State = state {
    onEntry {
        furhat.ask({
            random {
                +"Hi"
                +"Hello"
            }
            +"there, nice to meet you"})
    }

    onResponse<GreetReply> {
        furhat.ask("How should I call you?")
    }

    onResponse {
        val userName = it.text
        users.current.put("name", userName)
        furhat.say("Nice to meet you, $userName!")
        var confirm = furhat.askYN("Do you want to start this session?")

        if(confirm) {
            goto(ValidatingEmotion)
        } else {
            goto(EndConversation)
        }
    }
}

