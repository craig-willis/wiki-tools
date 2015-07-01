package edu.gslis.wiki;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.lang.StringEscapeUtils;
import org.mediawiki.importer.DumpWriter;
import org.mediawiki.importer.Page;
import org.mediawiki.importer.Revision;
import org.mediawiki.importer.Siteinfo;
import org.sweble.wikitext.engine.CompiledPage;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration;
import org.xml.sax.InputSource;



/**
 * Implements the Mediawiki DumpWriter interface for 
 * SAX-based parsing of a wiki dumpfile.  Writes to stdout
 */
public class WikiToTREC  implements DumpWriter {

    String currentDoc = "";
    String currentTitle = null;
    
    org.sweble.wikitext.engine.Compiler compiler = null;
    SimpleWikiConfiguration wikicfg = null;
    
    public static void main(String[] args) throws Exception {
        WikiToTREC dumpWriter = new WikiToTREC();

        String input = args[0];
        System.err.println("Converting " + input);
        XmlDumpReader wikiReader = null;
                    
        Reader reader= null;
        try {
            // Use commons-compress to auto-detect compressed formats
            InputStream ois = new BufferedInputStream(new FileInputStream(input));
            InputStream is = new CompressorStreamFactory().createCompressorInputStream(ois);                
            System.err.println("Auto-detected format");
            reader = new InputStreamReader(is, "UTF-8");

        } catch (Exception e) {
            try { 
                InputStream ois = new BufferedInputStream(new FileInputStream(input));
                // Try XZ directly, for grins
                InputStream is = new XZCompressorInputStream(ois);
                System.err.println("Reading XZ compressed text");
                reader = new InputStreamReader(is, "UTF-8");
            } catch (Exception e2) {
                System.err.println("Assuming UTF-8 encoded text");
                // Treat as uncompressed raw XML
                reader = new InputStreamReader(new FileInputStream(input));                    
            }
        }    
        
        wikiReader = new XmlDumpReader(new InputSource(reader), dumpWriter);
        wikiReader.readDump();
    }
    
    public WikiToTREC() throws IOException {
        
        wikicfg = new SimpleWikiConfiguration(
                "classpath:/org/sweble/wikitext/engine/SimpleWikiConfiguration.xml");
        
        compiler = new org.sweble.wikitext.engine.Compiler(wikicfg);
    }
    
    public void close() throws IOException {        
    }

    public void writeStartWiki() throws IOException{        
    }
    public void writeEndWiki() throws IOException{
        
    }

    public void writeSiteinfo(Siteinfo info) throws IOException {
        
    }

    public void writeStartPage(Page page) throws IOException {
        currentDoc += "<DOC>\n";
//        String pageId = String.valueOf(page.Id);
//        currentDoc += "<DOCNO>" + pageId + "</DOCNO>\n";
        
        currentTitle = page.Title.Text;
    }
    
    public void writeEndPage() throws IOException {
        currentDoc += "</DOC>\n";
        System.out.println(currentDoc);
        currentDoc = "";
    }

    public void writeRevision(Revision revision) throws IOException {
        String wikitext = StringEscapeUtils.unescapeHtml(revision.Text);
        String output = "";
        try
        {
            PageTitle pageTitle = PageTitle.make(wikicfg, currentTitle);
            PageId pageId = new PageId(pageTitle, -1);
            CompiledPage cp = compiler.postprocess(pageId, wikitext, null);
            
            TextConverter p = new TextConverter(wikicfg, 80);
            output = (String) p.go(cp.getPage());
            
            System.err.println("Indexing " + pageTitle);
            currentDoc += "<DOCNO>" + pageTitle.getTitle() + "</DOCNO>\n";
            currentDoc += "<TITLE>" + pageTitle.getFullTitle() + "</TITLE>\n";

            output = currentTitle + "\n" + output;     
            
            currentDoc += "<TEXT>\n" + output + "\n" + "</TEXT>\n";        

            
        } catch (Exception e) {
            System.out.println("Failed to parse page" + currentTitle);
            e.printStackTrace();
        }
    }    
}
