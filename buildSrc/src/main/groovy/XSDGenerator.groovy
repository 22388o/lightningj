/************************************************************************
 *                                                                       *
 *  LightningJ                                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public License   *
 *  (LGPL-3.0-or-later)                                                  *
 *  License as published by the Free Software Foundation; either         *
 *  version 3 of the License, or any later version.                      *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import javax.xml.bind.JAXBContext
import javax.xml.bind.SchemaOutputResolver
import javax.xml.transform.Result
import javax.xml.transform.stream.StreamResult
import java.lang.reflect.Method


/**
 * Gradle task for generating XSD from generated JAXB annotations by WrapperClassGenerator
 *
 * Created by Philip Vendil.
 */
class XSDGenerator extends DefaultTask{

    List protocols

    def classpath

    String compileClasses = "build/classes/java/main"

    String generatedResourcesDir = "build/resources/main"

    String systemId = "http://SomeURL"

    @TaskAction
    def generate() {
        for(String protocol : protocols) {
            ProtocolSettings protocolSettings = new ProtocolSettings(protocol: protocol)

            JAXBContext jaxbContext = getJAXBContext(protocolSettings)
            ByteArraySchemaOutputResolver sor = new ByteArraySchemaOutputResolver()
            jaxbContext.generateSchema(sor)

            new File(generatedResourcesDir+ "/" + protocolSettings.getXSDName()).write(new String(sor.bytes,"UTF-8"))
        }
    }

    JAXBContext getJAXBContext(ProtocolSettings protocolSettings){
        List classPaths = classpath.split(":")
        def ncl = new GroovyClassLoader(this.class.classLoader)
        classPaths.each {
            ncl.addClasspath(it)
        }
        ncl.addClasspath(compileClasses)
        ncl.addClasspath(generatedResourcesDir)
        URL f = ncl.getResource(protocolSettings.getJAXBIndexResouceLocation())
        Class c = ncl.loadClass("javax.xml.bind.JAXBContext")

        Method m = c.getMethod("newInstance",String.class,ClassLoader.class)
        return  m.invoke(null,protocolSettings.getJaxbSrcDirectory(),ncl)
    }



    class ByteArraySchemaOutputResolver extends SchemaOutputResolver {

        ByteArrayOutputStream baos = new ByteArrayOutputStream()

        Result createOutput(String namespaceURI, String suggestedFileName) throws IOException {
            StreamResult result = new StreamResult(baos)
            result.setSystemId(systemId)
            return result
        }

        byte[] getBytes(){
            return baos.toByteArray()
        }

    }
}
