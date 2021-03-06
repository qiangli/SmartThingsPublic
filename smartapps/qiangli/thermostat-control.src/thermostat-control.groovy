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
        description: "Use a switch to turn thermostat on and off.",
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
        input("mode", "enum", options: ["Heat", "Cool", "Auto"], title: "Mode", defaultValue: "Auto")
        input("temperature", "number", title: "Temperature", defaultValue: "72")
        input("delay", "number", title: "Delay (seconds)", defaultValue: "60")
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
    log.debug "initialize..."

    subscribe(switch1, "switch.on", switchOnHandler)
    subscribe(switch1, "switch.off", switchOffHandler)
}

def switchOnHandler(evt) {
    log.debug "switchOnHandler $evt.name: $evt.value $state"

    unschedule()
    runIn(delay, turnOn)
}

def switchOffHandler(evt) {
    log.debug "switchOffHandler $evt.name: $evt.value $state"

    unschedule()
    runIn(delay, turnOff)
}

def turnOff() {
    log.debug "Turning off thermostat $state ..."

    if (isThermostatOff()) {
        log.debug "thermostat is already off, returning..."
        return
    }
    log.debug "Thermostat is on, turning off ..."

    thermostat.off()

    stat()
}

def turnOn() {
    log.debug "Turning on thermostat $mode $temperature $delay, $state ..."

    if (isThermostatOn()) {
        log.debug "thermostat is already on, returning..."
        return
    }
    log.debug "thermostat is off, turning on..."

    switch ("$mode") {
        case "Cool":
            thermostat.cool()
            thermostat.setCoolingSetpoint(temperature)
            break
        case "Heat":
            thermostat.heat()
            thermostat.setHeatingSetpoint(temperature)
            break
        case "Auto":
            thermostat.auto()
            break
    }

    stat()
}

private isThermostatOff() {
    def mode = thermostat.currentValue("thermostatMode")
    log.debug "isThermostatOff? mode: $mode"

    return ("off" == mode)
}

private isThermostatOn() {
    def mode = thermostat.currentValue("thermostatMode")
    log.debug "isThermostatOn? mode: $mode"

    return ("off" != mode)
}

private stat() {
    state.thermostatMode = thermostat.currentValue("thermostatMode")
    state.coolingSetpoint = thermostat.currentValue("coolingSetpoint")
    state.heatingSetpoint = thermostat.currentValue("heatingSetpoint")
    //state.thermostatSetpoint = thermostat.currentValue("thermostatSetpoint")
    state.temperature = thermostat.currentValue("temperature")

    log.debug "State: $state"
}
