package org.nahoft.nahoft.models

import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "messages", strict = false)
data class Messages constructor(
    @field:ElementList(entry = "message", inline = true)
    @param:ElementList(entry = "message", inline = true)
    val list: List<Message>? = null)