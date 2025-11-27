package furhatos.app.outletagentskill.flow.main

import furhatos.flow.kotlin.NullSafeUserDataDelegate
import furhatos.records.User

var User.conversation_history: List<Pair<String, String>> by NullSafeUserDataDelegate { emptyList() }

var User.history: String by NullSafeUserDataDelegate { "" }