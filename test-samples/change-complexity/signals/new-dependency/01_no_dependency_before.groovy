// Test Case 01: Control - No New Infrastructure Dependencies
// Before: Simple Groovy code without infrastructure imports

import groovy.json.JsonSlurper
import groovy.xml.XmlParser

class DataProcessor {
    
    def parseJson(String jsonStr) {
        def slurper = new JsonSlurper()
        return slurper.parseText(jsonStr)
    }
    
    def parseXml(String xmlStr) {
        def parser = new XmlParser()
        return parser.parseText(xmlStr)
    }
}

def processor = new DataProcessor()
println(processor.parseJson('{"name": "test"}'))
