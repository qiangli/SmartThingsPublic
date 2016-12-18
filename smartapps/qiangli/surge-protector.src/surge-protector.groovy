/**
 *  Surge Protector
 *
 *  Author: Li Qiang
 *  Date: 2016-12-15
 */
definition(
        name: "Surge Protector",
        namespace: "qiangli",
        author: "Li Qiang",
        description: "Auto turn off the appliance if the total wattage exceeds the maximum to stop circuit breaker tripping.",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section("Maximum Wattage") {
        input("maximum", "number", title: "Wattage", required: true)
    }
    section("Appliance and its Wattage") {
        input("outlet1", "capability.switch", multiple: false)
        input("wattage1", "number", title: "Wattage", required: false, hideWhenEmpty: "outlet1")
        input("outlet2", "capability.switch", multiple: false)
        input("wattage2", "number", title: "Wattage", required: false, hideWhenEmpty: "outlet2")
        input("outlet3", "capability.switch", multiple: false)
        input("wattage3", "number", title: "Wattage", required: false, hideWhenEmpty: "outlet3")
    }
    section("Notifications") {
        input("sendPush", "bool", title: "Send push notification", required: false)
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

    subscribe(outlet1, "switch", switchHandler)
    subscribe(outlet2, "switch", switchHandler)
    subscribe(outlet3, "switch", switchHandler)
}

def switchHandler(evt) {
    log.debug "switchHandler $evt.name: $evt.value"

    if (evt.value == "on") {
        //turn off if exceeding max
        if (exceedsWattage()) {
            log.debug "max wattage exceeded, turning $evt.displayName off"
            evt.device.off()
        }
    }
}

private totalWattage() {
    def o1 = (outlet1.currentValue('switch') == 'on')
    def w1 = wattage1 as Integer

    def o2 = (outlet2.currentValue('switch') == 'on')
    def w2 = wattage2 as Integer

    def o3 = (outlet3.currentValue('switch') == 'on')
    def w3 = wattage3 as Integer

    def w = 0
    if (o1) {
        w += w1
    }
    if (o2) {
        w += w2
    }
    if (o3) {
        w += w3
    }
    log.debug "total wattage: $w"

    return w
}

private exceedsWattage() {
    def total = totalWattage()
    def max = maximum as Integer

    log.debug "total wattage: $total, maxt: $max"

    return total > max
}