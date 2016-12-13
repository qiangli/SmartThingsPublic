/**
 *  Time to Close
 *
 *  Author: Li Qiang
 *  Date: 2016-12-12
 */
definition(
        name: "Time to Close",
        namespace: "qiangli",
        author: "Li Qiang",
        description: "Notify when doors/windows are left open between sunset and sunrise.",
        category: "Safety & Security",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage%402x.png"
)

preferences {
    section("Doors/Windows") {
        input("sensors", "capability.contactSensor", title: "Contact sensors", multiple: true, required: true)
    }

    section("Notifications") {
        input("alarms", "capability.alarm", title: "Sirens", multiple: true, required: false)
        input("silent", "enum", options: ["Yes", "No"], title: "Strobe only (Yes/No)")
        input("lights", "capability.switch", title: "Flash lights", multiple: true, required: false)
        input("times", "number", title: "Number of times to flash?", required: false)
    }
}

def installed() {
    log.debug "installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "updated with settings: ${settings}"

    unsubscribe()
    unschedule()

    stopAlarms()

    initialize()
}

def initialize() {
    subscribe(location, "sunset", sunsetHandler)
    subscribe(location, "sunrise", sunriseHandler)

    subscribe(sensors, 'contact', contactHandler)

    subscribe(alarms, "alarm", alarmHandler)

    subscribe(app, appHandler)

    checkOpenSensors()
}

def sunsetHandler(evt) {
    log.debug "turning on lights at sunset:  $evt.name: $evt.value"

    checkOpenSensors()
}

def sunriseHandler(evt) {
    log.debug "turning on lights at sunrise:  $evt.name: $evt.value"

    stopAlarms()
}

def contactHandler(evt) {
    log.debug "contactHandler: $evt.name: $evt.value"

    if (!isDark()) {
        log.debug "sunrise time. ignoring"

        stopAlarms()
        return
    }

    if (evt.value == "open") {
        startAlarms()
    } else if (evt.value == 'closed') {
        if (!isContactOpen()) {
            log.debug "all windows/doors are closed, stopping alarms..."
            stopAlarms()
        } else {
            log.debug "some window/door is still open."
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
    if (state.alarmActive) {
        flashLights()
        schedule(util.cronExpression(now() + 5000), "continueFlashing")
    }
}

private checkOpenSensors() {
    if (isDark() && isContactOpen()) {
        startAlarms()
    }
}

private startAlarms() {
    state.alarmActive = true

    if (silentAlarm()) {
        alarms?.strobe()
    } else {
        alarms?.both()
    }

    if (lights) {
        continueFlashing()
    }
}

private stopAlarms() {
    if (state.alarmActive) {
        alarms?.off()
        unschedule()
    }
    state.alarmActive = false
}

private isContactOpen() {
    def result = sensors.find() { it.currentValue('contact') == 'open' }
    log.debug "isContactOpen results: $result"

    return result
}

private silentAlarm() {
    silent?.toLowerCase() in ["yes", "true", "y"]
}

private isDark() {
    def ss = getSunriseAndSunset()
    def t = now()

    log.debug "sunrise: ${ss.sunrise} sunset: ${ss.sunset} now: ${t}"

    if (t < ss.sunrise.time || t > ss.sunset.time) {
        return true
    } else {
        return false
    }
}

private flashLights() {
    def onFor = 1000
    def offFor = 1000
    def numFlashes = times as Integer

    log.debug "Flashing $numFlashes times"

    if (numFlashes) {
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
}