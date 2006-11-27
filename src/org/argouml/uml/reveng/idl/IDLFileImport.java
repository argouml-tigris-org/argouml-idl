// $Id$
// Copyright (c) 2004-2006 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies. This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason. IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.argouml.uml.reveng.idl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.argouml.kernel.Project;
import org.argouml.moduleloader.ModuleInterface;
import org.argouml.uml.reveng.FileImportUtils;
import org.argouml.uml.reveng.ImportInterface;
import org.argouml.uml.reveng.ImportSettings;
import org.argouml.uml.reveng.ImporterManager;
import org.argouml.uml.reveng.java.Modeller;
import org.argouml.util.FileFilters;
import org.argouml.util.SuffixFilter;

import antlr.RecognitionException;
import antlr.TokenStreamException;

/**
 * This is the main class for the IDL import.
 *
 * @author Andreas Rueckert a_rueckert@gmx.net
 */
public class IDLFileImport implements ImportInterface, ModuleInterface {

    /////////////////////////////////////////////////////////
    // Instance variables

    // The current project.
    private Project currentProject = null;

    private ImportSettings mySettings;

    /**
     * Default constructor.
     */
    public IDLFileImport() {
        super();
    }

    /*
     * @see org.argouml.uml.reveng.ImportInterface#parseFile(org.argouml.kernel.Project, java.lang.Object, org.argouml.uml.reveng.ImportSettings)
     */
    public void parseFile(Project p, Object o, ImportSettings settings)
        throws ImportException {
        
	if (o instanceof File) {
	    File f = (File) o;
	    mySettings = settings;
	    startImport(p, f);
	}
    }

    /**
     * Start the import process for a project and a file.
     *
     * @param p The project, where the import results are added.
     * @param f The file to start with.
     * @throws ImportException wrapped exception containing original error
     */
    public void startImport(Project p, File f) throws ImportException {
	currentProject = p;

	// Process the current file. If it's a directory, process all the file
	// in it.
	processFile(f, true);

    }

    /**
     * Count all the files, we're going to process, so we can display a
     * progress bar.
     *
     * @param f The directory as a File.
     * @param subdirectories If <tt>true</tt> we process subdirectories.
     * @return The number of files to process.
     */
    private int countFiles(File f, boolean subdirectories) {
	if (f.isDirectory() && subdirectories) {
	    return countDirectory(f);
	} else {
	    if (f.getName().endsWith(".idl")) {
		return 1;
	    }
	    return 0;
	}
    }

    /**
     * Count the files to process in a directory.
     *
     * @param f  The directory as a File.
     * @return The number of files in that directory.
     */
    private int countDirectory(File f) {
	int total = 0;
	String[] files = f.list(); // Get the content of the directory

	for (int i = 0; i < files.length; i++) {
	    total
		+= countFiles(
			      new File(f, files[i]),
			      mySettings.isDescendSelected());
	}
	return total;
    }

    /**
     * The main method for all parsing actions. It calls the actual parser
     * methods depending on the type of the file.
     *
     * @param f The file or directory, we want to parse.
     * @param subdirectories If <tt>true</tt> we process subdirectories.
     * @throws ImportException wrapped exception containing original error
     */
    public void processFile(File f, boolean subdirectories)
        throws ImportException {

	if (f.isDirectory()
	    && subdirectories) { // If f is a directory and the subdirectory
	    // flag is set,
	    processDirectory(f); // import all the files in this directory
	} else {

	    if (isParseable(f)) {
		String fileName = f.getName();
		try {
		    parseFile(new FileInputStream(f), fileName);
		} catch (FileNotFoundException e) {
                    throw new ImportException("File: " + fileName , e);
		}
	    }
	}
    }

    /**
     * This method imports an entire directory. It calls the parser for files
     * and creates packages for the directories.
     *
     * @param f The directory.
     * @throws ImportException wrapped exception containing original error
     */
    protected void processDirectory(File f) throws ImportException {
	boolean doSubdirs = mySettings.isDescendSelected();

	String[] files = f.list(); // Get the content of the directory

	for (int i = 0; i < files.length; i++) {
	    processFile(new File(f, files[i]), doSubdirs);
	}
    }


    /**
     * This method parses a single IDL source file.
     *
     * @param is The InputStream for the file to parse.
     * @param fileName The name of the parsed file.
     * @throws ImportException 
     */
    public void parseFile(InputStream is, String fileName)
        throws ImportException {

	int lastSlash = fileName.lastIndexOf('/');
	if (lastSlash != -1) {
	    fileName = fileName.substring(lastSlash + 1);
	}

	IDLParser parser =
	    new IDLParser(new IDLLexer(new BufferedInputStream(is)));

	// Create a modeller for the parser
	Modeller modeller = new Modeller(currentProject.getModel(),
	        			 mySettings.getDiagramInterface(),
	        			 mySettings.getImportSession(),
	        			 mySettings.isAttributeSelected(),
	        			 mySettings.isDatatypeSelected(),
	        			 fileName);
	// start parsing at the classfile rule
	try {
	    parser.specification(modeller);
	} catch (RecognitionException e) {
            throw new ImportException("File: " + fileName , e);
	} catch (TokenStreamException e) {
            throw new ImportException("File: " + fileName , e);
	}
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#enable()
     */
    public boolean enable() {
        ImporterManager.getInstance().addimporter(this);
        return true;
    }
    
    /*
     * @see org.argouml.moduleloader.ModuleInterface#disable()
     */
    public boolean disable() {
        return true;
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getName()
     */
    public String getName() {
        return "IDL";
    }
    
    /*
     * @see org.argouml.moduleloader.ModuleInterface#getInfo(int)
     */
    public String getInfo(int type) {
        switch (type) {
        case DESCRIPTION:
            return "Java import from IDL files";
        case AUTHOR:
            return "Andreas Rueckert";
        case VERSION:
            return "0.2 - $Id$";
        default:
            return null;
        }
    }

    /*
     * @see org.argouml.uml.reveng.ImportInterface#getSuffixFilters()
     */
    public SuffixFilter[] getSuffixFilters() {
	SuffixFilter[] result = {
	    FileFilters.IDL_FILTER,
	};
	return result;
    }

    /*
     * @see org.argouml.uml.reveng.ImportInterface#isParseable(java.io.File)
     */
    public boolean isParseable(File file) {
        return FileImportUtils.matchesSuffix(file, getSuffixFilters());
    }

    /*
     * @see org.argouml.uml.reveng.ImportInterface#getImportSettings()
     */
    public List getImportSettings() {
        return null;
    }
}
