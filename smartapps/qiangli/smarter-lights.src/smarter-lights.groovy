
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
        description: "Contact and motion sensors work in concert to control lights for garage or rooms with multiple doors.",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
    section("Turn on the lights after sunset and before sunrise"){
        input "lights", "capability.switch", multiple: true, title: "Lights", required: true
    }
    section("Offset before sunset/after sunrise") {
        input "offset", "text", title: "HH:MM", required: false, defaultValue: "01:00"
    }
    section("When one of the doors is open"){
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

    subscribe(doors, "contact.open", contactOpenHandler)
    subscribe(motions, "motion.active", motionActiveHandler)
}

def contactOpenHandler(evt) {
    log.debug "Contact open $evt.name: $evt.value"

    turnOnLights()
}

def motionActiveHandler(evt) {
    log.debug "Motion active $evt.name: $evt.value"

    turnOnLights()
}

private turnOnLights() {
    log.debug "turn On Lights"

    if (isDark()) {
        log.debug "It is dark, turning on..."

        lights.on()

        def delay = minutes * 60
        log.debug "Turning off in ${minutesLater} minutes (${delay}seconds)"
        runIn(delay, turnOffLights, [overwrite: true])
    }
}

private turnOffLights() {
    lights.off()
}

private isDark() {
    //def ss = getSunriseAndSunset()
    def sunriseOffset = "$offset"
    def sunsetOffset = "-$offset"
    def ss =getSunriseAndSunset(sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)

    def t = now()

    log.debug "sunrise: ${ss.sunrise} sunset: ${ss.sunset} now: ${t} offset: $offset"

    def dark = t < ss.sunrise.time || t > ss.sunset.time
    log.debug "dark = $dark"
    return dark
}