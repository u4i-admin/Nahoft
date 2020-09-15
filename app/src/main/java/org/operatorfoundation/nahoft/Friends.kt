package org.operatorfoundation.nahoft

import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "friends", strict = false)
data class Friends constructor(
    @field:ElementList(entry = "friend", inline = true)
    @param:ElementList(entry = "friend", inline = true)
    val list: List<Friend>? = null)