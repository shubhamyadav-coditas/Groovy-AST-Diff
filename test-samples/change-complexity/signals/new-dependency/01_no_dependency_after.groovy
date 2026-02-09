// Test Case 01: Control - No New Infrastructure Dependencies
// After: Added more Groovy standard library imports (no infra)

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.xml.XmlParser
import groovy.xml.MarkupBuilder

class DataProcessor {
    
    def parseJson(String jsonStr) {
        def slurper = new JsonSlurper()
        return slurper.parseText(jsonStr)
    }
    
    def toJson(Object obj) {
        return JsonOutput.toJson(obj)
    }
    
    def parseXml(String xmlStr) {
        def parser = new XmlParser()
        return parser.parseText(xmlStr)
    }
    
    def buildXml(Closure builder) {
        def writer = new StringWriter()
        def markup = new MarkupBuilder(writer)
        builder.delegate = markup
        builder()
        return writer.toString()
    }
}

def processor = new DataProcessor()
println(processor.parseJson('{"name": "test"}'))
