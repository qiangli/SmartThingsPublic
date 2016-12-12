/**
 *  Smart HVAC Sounder
 *
 *  Author: Qiang Li
 *  Date: 2016-12-08
 */
definition(
        name: "Smart HVAC Sounder",
        namespace: "qiangli",
        author: "Li Qiang",
        description: "Sound sirens, flash lights, or push notification if thermostat is on and windows/doors are open",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-IsItSafe.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-IsItSafe@2x.png"
)

preferences {
    section("HVAC") {
        input("thermostat", "capability.thermostat", title: "Thermostat", required: true)
    }

    section("Doors/Windows") {
        input("sensors", "capability.contactSensor", title: "Contact sensors", multiple: true, required: true)
    }

    section("Notifications") {
        input("alarms", "capability.alarm", title: "Alarms", multiple: true, required: false)
        input("silent", "enum", options: ["Yes", "No"], title: "Silent alarm only (Yes/No)")
        input("lights", "capability.switch", title: "Flash lights", multiple: true, required: false)
        input("sendPush", "bool", title: "Send Push Notification", required: false)
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    unschedule()

    stopAlarms()

    initialize()
}

def initialize() {
    subscribe(thermostat, 'thermostatMode', thermostatHandler)
    subscribe(sensors, 'contact', contactHandler)
    subscribe(alarms, "alarm", alarmHandler)

    subscribe(app, appHandler)
}

def thermostatHandler(evt) {
    log.debug "thermostatHandler: $evt.name: $evt.value"

    if (evt.value == "off") {
        log.debug "$evt.name thermostat is off."

        stopAlarms()
        return
    }

    //thermostat is on
    if (isContactOpen()) {
        log.debug "Something is open, start alarms..."

        startAlarms()
    }
}

def contactHandler(evt) {
    log.debug "contactHandler: $evt.name: $evt.value"

    if (isThermostatOff()) {
        log.debug "$evt.name thermostat is off."

        stopAlarms()
        return
    }

    //thermostat is on
    if (evt.value == "open") {
        startAlarms()
    } else if (evt.value == 'closed') {
        if (!isContactOpen()) {
            log.debug "Everything is closed, stopping alarms..."
            stopAlarms()
        } else {
            log.debug "Something is still open."
        }
    }
}

def alarmHandler(evt) {
    log.debug "alarmHandler: $evt.name: $evt.value"

    if (evt.value == "off") {
        stopAlarms()
    }
}

def appHandler(evt) {
    log.debug "appHandler: $evt.name: $evt.value"

    stopAlarms()
}

def continueFlashing() {
    unschedule()
    flashLights(3)
    schedule(util.cronExpression(now() + 5000), "continueFlashing")
}

private startAlarms() {
    if (silentAlarm()) {
        alarms?.strobe()
    } else {
        alarms?.both()
    }

    if (lights) {
        continueFlashing()
    }

    if (sendPush) {
        def cnames = openContactNames()
        def tname = thermostat.displayName
        def message = "Thermostat $tname is on and windows/doors ($cnames) are open"
        log.debug "sendPush: $message"

        sendPush(message)
    }
}

private stopAlarms() {
    alarms?.off()
    unschedule()
}

private isContactOpen() {
    def result = sensors.find() { it.currentValue('contact') == 'open' }
    log.debug "isOpen results: $result"

    return result
}

private openContactNames() {
    def result = sensors
            .findAll({ it.currentValue('contact') == 'open' })
            .collect({ it.displayName })
            .join(", ")
    log.debug "openContactNames results: $result"

    return result
}

private silentAlarm() {
    silent?.toLowerCase() in ["yes", "true", "y"]
}

private isThermostatOff() {
    def thermostatMode = thermostat.currentValue("thermostatMode")
    log.debug "thermostatMode $thermostatMode"

    return thermostatMode == "off"
}

private flashLights(numFlashes) {
    def onFor = 1000
    def offFor = 1000

    log.debug "FLASHING $numFlashes times"

    def delay = 1L
    numFlashes.times {
        log.trace "Switch on after  $delay msec"
        lights?.on(delay: delay)
        delay += onFor
        log.trace "Switch off after $delay msec"
        lights?.off(delay: delay)
        delay += offFor
    }
}