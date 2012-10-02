Build tools for Scala IDE and the ecosystem
===========================================

UpdateScalaIDEManifests
-----------------------

UpdateAddonManifests
--------------------

::

       <profile>
         <!-- pull the data needed to add the version numbers to the manifests -->
         <id>set-versions</id>
         <dependencies>
           <dependency>
             <groupId>org.scala-ide</groupId>
             <artifactId>build-tools_2.9.1</artifactId>
             <version>0.1.0-SNAPSHOT</version>
           </dependency>
         </dependencies>
         <build>
           <plugins>
             <plugin>
               <groupId>org.codehaus.mojo</groupId>
               <artifactId>exec-maven-plugin</artifactId>
               <version>1.2.1</version>
               <executions>
                 <execution>
                   <id>copy.reflect</id>
                   <goals>
                     <goal>java</goal>
                   </goals>
                 </execution>
               </executions>
               <configuration>
                 <classpathScope>compile</classpathScope>
                 <mainClass>org.scalaide.buildtools.UpdateAddonManifests</mainClass>
                 <arguments>
                   <argument>${repo.scala-ide}</argument>
                 </arguments>
               </configuration>
             </plugin>
           </plugins>
         </build>
         <repositories>
           <repository>
             <!-- extra repository containing the build package -->
             <id>typesafe-ide</id>
             <name>Typesafe IDE repository</name>
             <url>http://repo.typesafe.com/typesafe/ide-2.9</url>
             <snapshots><enabled>true</enabled></snapshots>
           </repository>
         </repositories>
       </profile>
 
