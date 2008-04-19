// $Id: TestJavaImportClass.java 10010 2006-03-10 07:45:56Z linus $
// Copyright (c) 1996-2008 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
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

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.argouml.model.InitializeModel;
import org.argouml.model.Model;
import org.argouml.profile.init.InitProfileSubsystem;
import org.argouml.uml.reveng.DummyImportSettings;

import antlr.RecognitionException;
import antlr.TokenStreamException;

/**
 * Test case to test the import of a IDL source file. 
 * 
 * This basically only tests that the basic import operation
 * succeeds at this point.
 */
public class TestIdlImport extends TestCase {
    
    static final String FILENAME = "test.idl";
    static final String PACKAGE_NAME = "MainModule";
    static final String CLASS_NAME = "Bar";

    /**
     * Flag, if the source is parsed already.
     */
    private static boolean isParsed;

    /**
     * Instances of the model and it's components.
     */
    private static Object parsedModel;
    private static Object parsedPackage;
    private static Object parsedClass;
    
    /*
     * @see junit.framework.TestCase#TestCase(String)
     */
    public TestIdlImport(String str) {
        super(str);
        InitializeModel.initializeDefault();
    }

    /*
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() {
        if (isParsed) {
            return;
        }

        InputStream is = TestIdlImport.class.getResourceAsStream(
                FILENAME);
        IDLLexer lexer = new IDLLexer(is);
        assertNotNull("Creation of lexer failed.", lexer);

        IDLParser parser = new IDLParser(lexer);
        assertNotNull("Creation of parser failed.", parser);

        parsedModel = Model.getModelManagementFactory().createModel();
        assertNotNull("Creation of model failed.", parsedModel);

        Model.getModelManagementFactory().setRootModel(parsedModel);

        new InitProfileSubsystem().init();
        
        Modeler modeler = new Modeler(parsedModel,
				new DummyImportSettings(), "test.idl");
        assertNotNull("Creation of Modeler instance failed.", modeler);

        try {
            parser.specification(modeler);
            isParsed = true;
        } catch (RecognitionException e) {
            fail("Parsing of IDL source failed." + e);
        } catch (TokenStreamException e) {
            fail("Parsing of IDL source failed." + e);
        }
    }

    /**
     * Test if the package was processed correctly.
     */
    public void testPackage() {
        lookupPackage();
        assertEquals("Inconsistent package name.",
                PACKAGE_NAME, Model.getFacade().getName(parsedPackage));
        assertEquals("The namespace of the package should be the model.",
                parsedModel, Model.getFacade().getNamespace(parsedPackage));
        assertTrue("The package should be recognized as a package.",
                Model.getFacade().isAPackage(parsedPackage));
    }

    /**
     * Test if the import was processed correctly.
     */
    public void no_testImport() {
        lookupPackage();
        Collection ownedElements =
            Model.getFacade().getOwnedElements(parsedPackage);
        assertNotNull("No elements owned by \"" + PACKAGE_NAME + "\".",
                ownedElements);
        Object component = null;
        Iterator iter = ownedElements.iterator();
        while (iter.hasNext()) {
            Object element = iter.next();
            if (Model.getFacade().isAComponent(element)) {
                component = element;
                break;
            }
        }
        assertNotNull("No component found.", component);
        assertEquals("The component name is wrong.",
            "TestClass.java", Model.getFacade().getName(component));
        Collection dependencies =
            Model.getFacade().getClientDependencies(component);
        assertNotNull("No dependencies found for component.", dependencies);
        Object permission = null;
        iter = dependencies.iterator();
        while (iter.hasNext()) {
            Object element = iter.next();
            if (Model.getFacade().isAPermission(element)) {
                permission = element;
                break;
            }
        }
        assertNotNull("No import found.", permission);
        assertEquals("The import name is wrong.",
            "TestClass.java -> Observer",
            Model.getFacade().getName(permission));
        Collection suppliers = Model.getFacade().getSuppliers(permission);
        assertNotNull("No suppliers found in import.", suppliers);
        Object supplier = null;
        iter = suppliers.iterator();
        if (iter.hasNext()) {
            supplier = iter.next();
        }
        assertNotNull("No supplier found in import.", supplier);
        assertEquals("The import supplier name is wrong.",
            "Observer", Model.getFacade().getName(supplier));
        Object namespace = Model.getFacade().getNamespace(supplier);
        assertNotNull("The import supplier has no namespace.", namespace);
        assertEquals("Expected namespace name \"util\".",
            "util", Model.getFacade().getName(namespace));
        namespace = Model.getFacade().getNamespace(namespace);
        assertNotNull("The namespace \"util\" has no namespace.", namespace);
        assertEquals("Expected namespace name \"java\".",
            "java", Model.getFacade().getName(namespace));
        assertEquals("The namespace of \"java\" should be the model.",
            parsedModel, Model.getFacade().getNamespace(namespace));
    }

    /**
     * Test if the interface declaration was imported correctly - basics only.
     */
    public void testSimpleClass() {
        lookupPackage();
        lookupClass();
        assertEquals("Inconsistent class name.",
            CLASS_NAME, Model.getFacade().getName(parsedClass));
        assertEquals("The namespace of the class should be\"" + PACKAGE_NAME
                + "\".", parsedPackage, Model.getFacade().getNamespace(
                parsedClass));
        assertTrue("The class should be recognized as a class.",
                Model.getFacade().isAClass(parsedClass));
        assertFalse("The class should not be abstract.",
                Model.getFacade().isAbstract(parsedClass));
        assertTrue("The class should be public.",
                Model.getFacade().isPublic(parsedClass));
    }
    
    /**
     * Test if the interface definition was processed correctly.
     */
    public void no_testClass() {
        lookupPackage();
        lookupClass();
        assertEquals("Inconsistent class name.",
            CLASS_NAME, Model.getFacade().getName(parsedClass));
        assertEquals("The namespace of the class should be\"" + PACKAGE_NAME
                + "\".", parsedPackage, Model.getFacade().getNamespace(
                parsedClass));
        assertTrue("The class should be recognized as a class.",
                Model.getFacade().isAClass(parsedClass));
        assertFalse("The class should not be abstract.",
                Model.getFacade().isAbstract(parsedClass));
        assertTrue("The class should be public.",
                Model.getFacade().isPublic(parsedClass));
        Collection generalizations =
            Model.getFacade().getGeneralizations(parsedClass);
        assertNotNull("No generalizations found for class.", generalizations);
        Object generalization = null;
        Iterator iter = generalizations.iterator();
        if (iter.hasNext()) {
            generalization = iter.next();
        }
        assertNotNull("No generalization found for class.", generalization);
        assertEquals("The generalization name is wrong.",
            "TestClass -> Object", Model.getFacade().getName(generalization));
        assertEquals("The child of the generalization should be the class.",
            parsedClass, Model.getFacade().getSpecific(generalization));
        assertEquals("The parent of the generalization should be \"Object\".",
            "Object",
            Model.getFacade().getName(
                        Model.getFacade().getGeneral(generalization)));
        Collection dependencies =
            Model.getFacade().getClientDependencies(parsedClass);
        assertNotNull("No dependencies found for class.", dependencies);
        Object abstraction = null;
        iter = dependencies.iterator();
        if (iter.hasNext()) {
            abstraction = iter.next();
        }
        assertNotNull("No abstraction found for class.", abstraction);
        assertEquals("The abstraction name is wrong.",
            "TestClass -> Observer", Model.getFacade().getName(abstraction));
        try {
            assertEquals("The client of the abstraction should be the class.",
                parsedClass,
                Model.getFacade().getClients(abstraction)
                            .iterator().next());
        } catch (IllegalArgumentException ex) {
            fail("The implementation dependency has no clients.");
        }
        try {
            assertEquals(
                    "The supplier of the abstraction should be \"Observer\".",
                    "Observer", Model.getFacade().getName(
                            Model.getFacade().getSuppliers(abstraction)
                                    .iterator().next()));
        } catch (IllegalArgumentException ex) {
            fail("The abstraction has no suppliers.");
        }
    }


    /**
     * Test if the attributes were processed correctly.
     */
    public void no_testAttributes() {
        lookupPackage();
        lookupClass();
        Collection attributes = Model.getFacade().getAttributes(parsedClass);
        assertNotNull("No attributes found ib class.", attributes);
        assertEquals("Number of attributes is wrong", 2, attributes.size());
        Object attribute = null;
        Object attributeForn = null;
        Object attributeFors = null;
        Iterator iter = attributes.iterator();
        while (iter.hasNext()) {
            attribute = iter.next();
            assertTrue("The attribute should be recognized as an attribute.",
                    Model.getFacade().isAAttribute(attribute));
            if ("n".equals(Model.getFacade().getName(attribute))) {
                attributeForn = attribute;
            } else if ("s".equals(Model.getFacade().getName(attribute))) {
                attributeFors = attribute;
            }
        }
        assertTrue("The attributes have wrong names.",
                attributeForn != null && attributeFors != null);
        Object attribType = Model.getFacade().getType(attributeForn);
        Object initializer = Model.getFacade().getInitialValue(attributeForn);
        assertTrue("Attribute n should be private.",
                Model.getFacade().isPrivate(attributeForn));
        assertFalse("Attribute n should not be final.",
                Model.getFacade().isReadOnly(attributeForn));
        assertTrue("Attribute n should have type int.",
                "int".equals(Model.getFacade().getName(attribType)));
        assertTrue("Attribute n has no initializer.",
                Model.getFacade().isInitialized(attributeForn)
                && initializer != null);
        assertEquals("The initializer of attribute n is wrong.",
            " 0", Model.getFacade().getBody(initializer));
        attribType = Model.getFacade().getType(attributeFors);
        initializer = Model.getFacade().getInitialValue(attributeFors);
        assertTrue("Attribute s should be public.",
                Model.getFacade().isPublic(attributeFors));
        assertTrue("Attribute s should be static.",
                Model.getFacade().isStatic(attributeFors));
        assertTrue("Attribute s should be final.",
                Model.getFacade().isReadOnly(attributeFors));
        assertTrue("Attribute s should have type String.",
                "String".equals(Model.getFacade().getName(attribType)));
        assertTrue("Attribute s has no initializer.",
                Model.getFacade().isInitialized(attributeFors)
                && initializer != null);
        assertEquals("The initializer of attribute s is wrong.",
            " \"final String object\"", Model.getFacade().getBody(initializer));
    }

    /**
     * Test if the association was processed correctly.
     */
    public void no_testAssociation() {
        lookupPackage();
        lookupClass();
        Collection associationEnds =
            Model.getFacade().getAssociationEnds(parsedClass);
        assertNotNull("No association ends found ib class.", associationEnds);
        assertEquals("Number of association ends is wrong", 2,
                associationEnds.size());
        Object associationEnd = null;
        Object association = null;
        boolean unidirectional = false;
        Iterator iter = associationEnds.iterator();
        while (iter.hasNext()) {
            associationEnd = iter.next();
            assertTrue(
                    "The attribute end should be recognized as "
                    + "an attribute end.",
                    Model.getFacade().isAAssociationEnd(associationEnd));
            assertEquals("The type of both association ends must be the class.",
                parsedClass, Model.getFacade().getType(associationEnd));
            if (association == null) {
                association = Model.getFacade().getAssociation(associationEnd);
                assertTrue(
                        "The attribute should be recognized as an attribute.",
                        Model.getFacade().isAAssociation(association));
                assertEquals("The association name is wrong.",
                    "TestClass -> TestClass",
                    Model.getFacade().getName(association));
            } else {
                assertEquals(
                        "Association end must belong to the same association.",
                        association,
                        Model.getFacade().getAssociation(associationEnd));
            }
            if (Model.getFacade().isNavigable(associationEnd)) {
                unidirectional = !unidirectional;
            }
        }
        assertTrue("Only one association end must be navigable.",
                unidirectional);
    }

    /**
     * Test if the operations were processed correctly.
     */
    public void no_testOperations() {
        lookupPackage();
        lookupClass();
        Collection operations = Model.getFacade().getOperations(parsedClass);
        assertNotNull("No operations found in class.", operations);
        assertEquals("Number of operations is wrong", 4, operations.size());
        Object operation = null;
        Object operationForTestClass = null;
        Object operationForupdate = null;
        Object operationForgetString = null;
        Object operationForx = null;
        Iterator iter = operations.iterator();
        while (iter.hasNext()) {
            operation = iter.next();
            assertTrue("The operation should be recognized as an operation.",
                    Model.getFacade().isAOperation(operation));
            if (CLASS_NAME.equals(Model.getFacade().getName(operation))) {
                operationForTestClass = operation;
            } else if ("update".equals(Model.getFacade().getName(operation))) {
                operationForupdate = operation;
            } else if ("getString".equals(
                    Model.getFacade().getName(operation))) {
                operationForgetString = operation;
            } else if ("x".equals(Model.getFacade().getName(operation))) {
                operationForx = operation;
            }
        }
        assertTrue("The operations have wrong names.",
            operationForTestClass != null
            && operationForupdate != null
            && operationForgetString != null
            && operationForx != null);
        assertTrue("Operation TestClass should be protected.",
                Model.getFacade().isProtected(operationForTestClass));
//        assertEquals("The body of operation TestClass is wrong.",
//                BODY2, getBody(operationForTestClass));
        assertTrue("Operation update should be public.",
                Model.getFacade().isPublic(operationForupdate));
        assertTrue("Operation getString should be static.",
                Model.getFacade().isStatic(operationForgetString));
        assertTrue("Operation getString should be private.",
                Model.getFacade().isPrivate(operationForgetString));
        assertTrue("Operation x should be abstract.",
                Model.getFacade().isAbstract(operationForx));
        assertTrue("Operation x should have package visibility.",
                Model.getFacade().isPackage(operationForx));
    }
    
    /**
     * Lookup package if we don't have it.
     */
    private void lookupPackage() {
        if (parsedPackage == null) {
            parsedPackage = Model.getFacade().lookupIn(parsedModel,
                    PACKAGE_NAME);
            assertNotNull(
                    "No package \"" + PACKAGE_NAME + "\" found in model.",
                    parsedPackage);
        }
    }

    /**
     * Lookup our test class in the model if we don't already have it.
     */
    private void lookupClass() {
        if (parsedClass == null) {
            parsedClass =
                Model.getFacade().lookupIn(parsedPackage, CLASS_NAME);
            assertNotNull("No class \"" + CLASS_NAME + "\" found.", 
                    parsedClass);
        }
    }


}