/**
 * based on left-it-open.groovy
 */
definition(
        name: "Garage Left Open",
        namespace: "qiangli",
        author: "Li Qiang",
        description: "Monitor your garage door (contact sensor) and get a text message if it is open too long",
        category: "Safety & Security",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {

    section("Monitor this garage door") {
        input "contact", "capability.contactSensor"
    }
    section("And notify me if it's open for more than this many minutes (default 1)") {
        input "openThreshold", "number", description: "Number of minutes", required: false
    }
    section("Delay between notifications (default 1 minutes") {
        input "frequency", "number", title: "Number of minutes", description: "", required: false
    }
    section("Via text message at this number (or via push notification if not specified") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Phone number (optional)", required: false
        }
    }
}

def getOpenThreshold() {
    return (openThreshold != null && openThreshold != "") ? openThreshold : 1
}

def getFrequency() {
    return (frequency != null && frequency != "") ? frequency : 1
}

def installed() {
    log.trace "installed()"
    subscribe()
}

def updated() {
    log.trace "updated()"
    unsubscribe()
    subscribe()
}

def subscribe() {
    subscribe(contact, "contact.open", doorOpen)
    subscribe(contact, "contact.closed", doorClosed)
}

def doorOpen(evt) {
    log.trace "doorOpen($evt.name: $evt.value)"
    def t0 = now()
    def delay = getOpenThreshold() * 60
    runIn(delay, doorOpenTooLong, [overwrite: false])
    log.debug "scheduled doorOpenTooLong in ${now() - t0} msec"
}

def doorClosed(evt) {
    log.trace "doorClosed($evt.name: $evt.value)"
}

def doorOpenTooLong() {
    def contactState = contact.currentState("contact")
    def freq = getFrequency() * 60

    if (contactState.value == "open") {
        def elapsed = now() - contactState.rawDateCreated.time
        def threshold = (getOpenThreshold() * 60000) - 1000
        if (elapsed >= threshold) {
            log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
            sendMessage()
            runIn(freq, doorOpenTooLong, [overwrite: false])
        } else {
            log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
        log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
    }
}

void sendMessage() {
    def minutes = getOpenThreshold()
    def msg = "${contact.displayName} has been left open for ${minutes} minutes."
    log.info msg
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    } else {
        if (phone) {
            sendSms phone, msg
        } else {
            sendPush msg
        }
    }
}
