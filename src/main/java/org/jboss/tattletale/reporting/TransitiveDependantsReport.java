/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.tattletale.reporting;

import org.jboss.tattletale.core.Archive;
import org.jboss.tattletale.core.ArchiveTypes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Transitive dependants report
 * @author Jesper Pedersen <jesper.pedersen@jboss.org>
 * @author <a href="mailto:torben.jaeger@jit-consulting.de">Torben Jaeger</a>
 */
public class TransitiveDependantsReport extends CLSReport
{
   /** NAME */
   private static final String NAME = "Transitive Dependants";

   /** DIRECTORY */
   private static final String DIRECTORY = "transitivedependants";


   /**
    * Constructor
    * @param archives The archives
    * @param classloaderStructure The classloader structure
    */
   public TransitiveDependantsReport(SortedSet<Archive> archives,
                                     String classloaderStructure)
   {
      super(DIRECTORY, ReportSeverity.INFO, archives, NAME, DIRECTORY, classloaderStructure);
   }

   /**
    * write out the report's content
    * @param bw the writer to use
    * @exception IOException if an error occurs
    */   
   void writeHtmlBodyContent(BufferedWriter bw) throws IOException
   {
      bw.write("<table>" + Dump.NEW_LINE);

      bw.write("  <tr>" + Dump.NEW_LINE);
      bw.write("     <th>Archive</th>" + Dump.NEW_LINE);
      bw.write("     <th>Dependants</th>" + Dump.NEW_LINE);
      bw.write("  </tr>" + Dump.NEW_LINE);

      SortedMap<String, SortedSet<String>> dependantsMap = new TreeMap<String, SortedSet<String>>();

      for (Archive archive : archives)
      {

         if (archive.getType() == ArchiveTypes.JAR)
         {
            SortedSet<String> result = new TreeSet<String>();

            for (Archive a : archives)
            {

               if (a.getType() == ArchiveTypes.JAR)
               {
                  for (String require : a.getRequires())
                  {

                     if (archive.doesProvide(require) && (getCLS() == null || getCLS().isVisible(a, archive)))
                     {
                        result.add(a.getName());
                     }
                  }
               }
            }

            dependantsMap.put(archive.getName(), result);
         }
      }

      SortedMap<String, SortedSet<String>> transitiveDependantsMap = new TreeMap<String, SortedSet<String>>();

      Iterator mit = dependantsMap.entrySet().iterator();
      while (mit.hasNext())
      {
         Map.Entry entry = (Map.Entry)mit.next();

         String archive = (String)entry.getKey();
         SortedSet<String> value = (SortedSet<String>)entry.getValue();

         SortedSet<String> result = new TreeSet<String>();

         if (value != null && value.size() > 0)
         {
            for (String aValue : value)
            {
               resolveDependants(aValue, archive, dependantsMap, result);
            }
         }

         transitiveDependantsMap.put(archive, result);
      }

      boolean odd = true;

      mit = transitiveDependantsMap.entrySet().iterator();
      while (mit.hasNext())
      {
         Map.Entry entry = (Map.Entry)mit.next();

         String archive = (String)entry.getKey();
         SortedSet<String> value = (SortedSet<String>)entry.getValue();

         if (odd)
         {
            bw.write("  <tr class=\"rowodd\">" + Dump.NEW_LINE);
         }
         else
         {
            bw.write("  <tr class=\"roweven\">" + Dump.NEW_LINE);
         }
         bw.write("     <td><a href=\"../jar/" + archive + ".html\">" + archive + "</a></td>" + Dump.NEW_LINE);
         bw.write("     <td>");

         if (value.size() == 0)
         {
            bw.write("&nbsp;");
         }
         else
         {
            Iterator<String> valueIt = value.iterator();
            while (valueIt.hasNext())
            {
               String r = valueIt.next();
               if (r.endsWith(".jar"))
               {
                  bw.write("<a href=\"../jar/" + r + ".html\">" + r + "</a>");
               }
               else
               {
                  bw.write("<i>" + r + "</i>");
               }

               if (valueIt.hasNext())
               {
                  bw.write(", ");
               }
            }
         }

         bw.write("</td>" + Dump.NEW_LINE);
         bw.write("  </tr>" + Dump.NEW_LINE);

         odd = !odd;
      }

      bw.write("</table>" + Dump.NEW_LINE);
   }

   /**
    * write out the header of the report's content
    * @param bw the writer to use
    * @throws IOException if an errror occurs
    */
   void writeHtmlBodyHeader(BufferedWriter bw) throws IOException
   {
      bw.write("<body>" + Dump.NEW_LINE);
      bw.write(Dump.NEW_LINE);

      bw.write("<h1>" + NAME + "</h1>" + Dump.NEW_LINE);

      bw.write("<a href=\"../index.html\">Main</a>" + Dump.NEW_LINE);
      bw.write("<p>" + Dump.NEW_LINE);
   }

   /**
    * Get dependants
    * @param scanArchive The scan archive
    * @param archive The archive
    * @param map The dependants map
    * @param result The result
    */
   private void resolveDependants(String scanArchive,
                                  String archive,
                                  SortedMap<String, SortedSet<String>> map,
                                  SortedSet<String> result)
   {
      if (!archive.equals(scanArchive) && !result.contains(scanArchive))
      {
         result.add(scanArchive);

         SortedSet<String> value = map.get(scanArchive);
         if (value != null)
         {
            for (String aValue : value)
            {
               resolveDependants(aValue, archive, map, result);
            }
         }
      }
   }
}
