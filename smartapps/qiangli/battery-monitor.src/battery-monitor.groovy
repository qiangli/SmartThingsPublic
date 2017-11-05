/**
 * Created by liqiang on 11/5/17.
 * based on https://github.com/notoriousbdg/SmartThings.BatteryMonitor/tree/master/smartapps/notoriousbdg/batterymonitor.src
 */
def smartAppNameFull() {
    return  "Battery Monitor SmartApp for SmartThings"
}

def smartAppNameShort() {
    return  "Battery Monitor"
}

def smartAppVersion() {
    return  "Version 0.0.1"
}

def smartAppAuthor() {
    return  "Qiang Li"
}

def smartAppCopyright() {
    return  "Copyright (c) 2017"
}

def smartAppSource() {
    return  "https://github.com/qiangli/SmartThingsPublic/tree/master/smartapps/qiangli/battery-monitor.src"
}

def smartAppDescription() {
    return  "Monitor the status of devices' batteries."
}

def smartAppRevision () {
    return  'v0.0.1\n'
}

def smartAppLicense() {
    return  'Licensed under the Apache License, Version 2.0 (the "License")'
}

definition(
        name: "Battery Monitor",
        namespace: "qiangli",
        author: "Qiang Li",
        description: "SmartApp to monitor battery levels.",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page name:"pageStatus"
    page name:"pageConfigure"
    page name:"pageAbout"
}

// Show About Page
def pageAbout() {
    def pageProperties = [
            name:           "pageAbout",
            title:          smartAppNameFull(),
            nextPage:       "pageConfigure",
            uninstall:      true
    ]

    return dynamicPage(pageProperties) {
        section() {
            paragraph smartAppVersion() + "\n" +
                    smartAppAuthor() + "\n" +
                    smartAppCopyright()
        }

        section() {
            paragraph smartAppDescription()
        }

        section() {
            href(
                    name: "sourceCode",
                    title: "Source Code (Tap to view)",
                    required: false,
                    external: true,
                    style: "external",
                    url: smartAppSource(),
                    description: smartAppSource()
            )
        }

        section() {
            paragraph title: "Revision History",
                    smartAppRevision()
        }

        section() {
            paragraph title: "License",
                    smartAppLicense()
        }
    }
}

// Show Status page
def pageStatus() {
    def pageProperties = [
            name:       "pageStatus",
            title:      smartAppNameShort() + " Status",
            nextPage:   null,
            install:    true,
            uninstall:  true
    ]

    if (settings.devices == null) {
        return pageAbout()
    }

    def listLevel0 = ""
    def listLevel1 = ""
    def listLevel2 = ""
    def listLevel3 = ""
    def listLevel4 = ""

    if (settings.level1 == null) { settings.level1 = 33 }
    if (settings.level3 == null) { settings.level3 = 67 }
    if (settings.pushMessage) { settings.pushMessage = true }

    return dynamicPage(pageProperties) {
        settings.devices.each() {
            try {
                if (it.currentBattery == null) {
                    listLevel0 += "$it.displayName\n"
                } else if (it.currentBattery >= 0 && it.currentBattery <  settings.level1.toInteger()) {
                    listLevel1 += "$it.currentBattery  $it.displayName\n"
                } else if (it.currentBattery >= settings.level1.toInteger() && it.currentBattery <= settings.level3.toInteger()) {
                    listLevel2 += "$it.currentBattery  $it.displayName\n"
                } else if (it.currentBattery >  settings.level3.toInteger() && it.currentBattery < 100) {
                    listLevel3 += "$it.currentBattery  $it.displayName\n"
                } else if (it.currentBattery == 100) {
                    listLevel4 += "$it.displayName\n"
                } else {
                    listLevel0 += "$it.currentBattery  $it.displayName\n"
                }
            } catch (e) {
                log.trace "Caught error checking battery status."
                log.trace e
                listLevel0 += "$it.displayName\n"
            }
        }

        if (listLevel0) {
            section("Batteries with errors or no status") {
                paragraph listLevel0.trim()
            }
        }

        if (listLevel1) {
            section("Batteries with low charge (less than $settings.level1)") {
                paragraph listLevel1.trim()
            }
        }

        if (listLevel2) {
            section("Batteries with medium charge (between $settings.level1 and $settings.level3)") {
                paragraph listLevel2.trim()
            }
        }

        if (listLevel3) {
            section("Batteries with high charge (more than $settings.level3)") {
                paragraph listLevel3.trim()
            }
        }

        if (listLevel4) {
            section("Batteries with full charge") {
                paragraph listLevel4.trim()
            }
        }

        section("Menu") {
            href "pageStatus", title:"Refresh", description:""
            href "pageConfigure", title:"Configure", description:""
            href "pageAbout", title:"About", description: ""
        }
    }
}

// Show Configure Page
def pageConfigure() {
    def helpPage =
            "Select devices with batteries that you wish to monitor."

    def inputBattery   = [
            name:           "devices",
            type:           "capability.battery",
            title:          "Which devices with batteries?",
            multiple:       true,
            required:       true
    ]

    def inputLevel1    = [
            name:           "level1",
            type:           "number",
            title:          "Low battery threshold?",
            defaultValue:   "20",
            required:       true
    ]

    def inputLevel3    = [
            name:           "level3",
            type:           "number",
            title:          "Medium battery threshold?",
            defaultValue:   "70",
            required:       true
    ]

    def inputTime      = [
            name:           "time",
            type:           "time",
            title:          "Notify at what time daily?",
            required:       false
    ]

    def inputPush      = [
            name:           "pushMessage",
            type:           "bool",
            title:          "Send push notifications?",
            defaultValue:   false
    ]

    def inputSMS       = [
            name:           "phoneNumbers",
            type:           "phone",
            title:          "Send SMS notifications? Enter one or more phone numbers separated by '*'",
            required:       false
    ]

    def pageProperties = [
            name:           "pageConfigure",
            title:          smartAppNameShort() + " Configuration",
            nextPage:       "pageStatus",
            uninstall:      true
    ]

    return dynamicPage(pageProperties) {
        section("About") {
            paragraph helpPage
        }

        section("Devices") {
            input inputBattery
        }

        section("Settings") {
            input inputLevel1
            input inputLevel3
        }

        section("Notification") {
            input inputTime
            input inputPush
            input inputSMS
        }

        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

def installed() {
    log.debug "Initialized with settings: ${settings}"
    initialize()
}

def updated() {
    unschedule()
    unsubscribe()
    initialize()
}

def initialize() {
    schedule(settings.time, updateStatus)
}

def send(msg) {
    log.debug msg

    if (settings.pushMessage) {
        sendPush(msg)
    } else {
        sendNotificationEvent(msg)
    }

    if (settings.phoneNumbers) {
        for (phone in phoneNumbers.split('\\*')) {
            if (phone) {sendSms(phone, msg)}
        }
    }
}

def updateStatus() {
    settings.devices.each() {
        try {
            if (it.currentBattery == null) {
                send("${it.displayName} battery is not reporting.")
            } else if (it.currentBattery > 100) {
                send("${it.displayName} battery is ${it.currentBattery}, which is over 100.")
            } else if (it.currentBattery < settings.level1) {
                send("${it.displayName} battery is ${it.currentBattery} (threshold ${settings.level1}.)")
            }
        } catch (e) {
            log.trace "Caught error checking battery status."
            log.trace e
            send("${it.displayName} battery reported a non-integer level.")
        }
    }
}
