
/**
 *  Smarter Lights
 *
 *  Author: Li Qiang
 *  Date: 2016-12-17
 */
definition(
        name: "Smarter Lights",
        namespace: "qiangli",
        author: "Li Qiang",
        description: "Contact and motion sensors work in concert to control lights for garage or rooms with multiple doors. At anytime, you may suspend the app by touching the play icon.",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
    section("Turn on the lights"){
        input "lights", "capability.switch", multiple: true, title: "Lights", required: true
        input "afterDark", "bool", title: "after sunset and before sunrise", required: false
        input "offset", hideWhenEmpty: "afterDark", "text", title: "Offset before sunset/after sunrise (HH:MM)", required: false, defaultValue: "01:00"
    }
    section("When door is open"){
        input "doors", "capability.contactSensor", multiple: true, title: "Open/Close contact sensors", required: false
    }
    section("or motion is detected"){
        input "motions", "capability.motionSensor", multiple: true, title: "Motion sensors", required: false
    }
    section("Automatically turn off the lights after") {
        input "minutes", "number", title: "Minutes later", defaultValue: "5"
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()

    initialize()
}

def initialize() {
    log.debug "initialize..."

    state.disabled = false

    subscribe(doors, "contact.open", contactOpenHandler)
    subscribe(doors, "contact.closed", contactClosedHandler)
    subscribe(motions, "motion.active", motionActiveHandler)
    subscribe(motions, "motion.inactive", motionInActiveHandler)

    subscribe(app, appHandler)
}

def appHandler(evt) {
    log.debug "app event: $evt.displayName, $evt.name: $evt.value, disabled: $state.disabled"

    state.disabled = !state.disabled

    log.debug "app disabled? $state.disabled"

    def msg = "$app.label is " + (state.disabled ? "disabled" : "enabled")
    sendPush msg
}

def contactOpenHandler(evt) {
    log.debug "Contact open $evt.displayName $evt.name: $evt.value"

    turnOnLights()
}

def contactClosedHandler(evt) {
    log.debug "contact closed $evt.displayName $evt.name: $evt.value"

    scheduleTurnOff()
}

def motionActiveHandler(evt) {
    log.debug "Motion active $evt.displayName $evt.name: $evt.value"

    turnOnLights()
}

def motionInActiveHandler(evt) {
    log.debug "Motion inactive $evt.displayName $evt.name: $evt.value"

    scheduleTurnOff()
}

def turnOnLights() {
    log.debug "turn on lights, disabled: $state.disabled"
    if (state.disabled) {
        return
    }

    if (autoLightOn()) {
        log.debug "turning on lights..."

        lights.on()
    }
}

def scheduleTurnOff() {
    log.debug "schedule turn off, disabled: $state.disabled"

    //schedule the turn off regardless of disabled state or not

    def delay = minutes * 60
    log.debug "Turning off in ${minutes} minutes (${delay}seconds)"
    runIn(delay, turnOffLights, [overwrite: true])
}

def turnOffLights() {
    log.debug "turning off lights, disabled: $state.disabled"
    if (state.disabled) {
        return
    }

    lights.off()
}

private autoLightOn() {
    log.debug "Auto light on, aferDark: $afterDark"

    if (!afterDark) {
        return true
    }

    //after dark only
    def sunriseOffset = "$offset"
    def sunsetOffset = "-$offset"
    def ss =getSunriseAndSunset(sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)

    def t = now()

    log.debug "sunrise: ${ss.sunrise} sunset: ${ss.sunset} now: ${t} offset: $offset"

    def dark = t < ss.sunrise.time || t > ss.sunset.time
    log.debug "dark? $dark"
    return dark
}
