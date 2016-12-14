/**
 *  Thermostat Control
 *
 *  Author: Li Qiang
 *  Date: 2016-12-12
 */
definition(
        name: "Thermostat Control",
        namespace: "qiangli",
        author: "Li Qiang",
        description: "When a switch turns on/off, turn on/off thermostat.",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
    section("When a switch turns on or off...") {
        input("switch1", "capability.switch", multiple: false)
    }

    section("HVAC") {
        input("thermostat", "capability.thermostat", title: "Thermostat", multiple: false)
        input("delay", "number", title: "Delay (seconds)")
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
    initialize()
}

def initialize() {
    subscribe(master, "switch", switchHandler)
}

def switchHandler(evt) {
    log.debug "Desc: $evt.value , $state"

    if(evt.value == 'off') {
        unschedule()
        runIn(delay, 'turnOff')
    } else if(evt.value == 'on') {
        unschedule()
        runIn(delay, 'restore')
    }
}

def turnOff() {
    log.debug "Turning off thermostat..."
    state.thermostatMode = thermostat.currentValue("thermostatMode")
    thermostat.off()
    log.debug "State: $state"
}

def restore() {
    log.debug "Setting thermostat to $state.thermostatMode"
    thermostat.setThermostatMode(state.thermostatMode)
}