package org.codehaus.plexus.languages.java.jpms;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Extract information from module with ASM
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public class AsmModuleInfoParser
    implements ModuleInfoParser
{
    @Override
    public JavaModuleDescriptor getModuleDescriptor( Path modulePath )
        throws IOException
    {
        JavaModuleDescriptor descriptor;
        if ( Files.isDirectory( modulePath ) )
        {
            try ( InputStream in = Files.newInputStream( modulePath.resolve( "module-info.class" ) ) ) 
            { 
                descriptor = parse( in );
            }
        }
        else
        {
            try ( JarFile jarFile = new JarFile( modulePath.toFile() ) )
            {
                JarEntry moduleInfo = jarFile.getJarEntry( "module-info.class" );
                
                if ( moduleInfo != null )
                {
                    descriptor = parse( jarFile.getInputStream( moduleInfo ) );
                }
                else
                {
                    descriptor = null;
                }
            }
        }
        return descriptor;
    }
    
    private JavaModuleDescriptor parse( InputStream in ) throws IOException
    {
        final JavaModuleDescriptorWrapper wrapper = new JavaModuleDescriptorWrapper();
        
        if ( in != null )
        {
            ClassReader reader = new ClassReader( in );
            reader.accept( new ClassVisitor( Opcodes.ASM6 )
            {
                @Override
                public ModuleVisitor visitModule( String name, int arg1, String arg2 )
                {
                    wrapper.builder = JavaModuleDescriptor.newModule( name );

                    return new ModuleVisitor( Opcodes.ASM6 )
                    {
                        @Override
                        public void visitRequire( String module, int access, String version )
                        {
                            wrapper.builder.requires( module );
                        }
                    };
                }
            }, 0 );
        }
        return wrapper.builder.build();
    }
    
    private static class JavaModuleDescriptorWrapper 
    {
        private JavaModuleDescriptor.Builder builder;
    }
}
