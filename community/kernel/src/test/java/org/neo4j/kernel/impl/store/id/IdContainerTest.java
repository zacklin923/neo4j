/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.store.id;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.impl.store.InvalidIdGeneratorException;
import org.neo4j.test.rule.TestDirectory;
import org.neo4j.test.rule.fs.DefaultFileSystemRule;
import org.neo4j.test.rule.fs.FileSystemRule;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


public class IdContainerTest
{
    @Rule
    public final TestDirectory testDirectory = TestDirectory.testDirectory();
    @Rule
    public final FileSystemRule fileSystemRule = new DefaultFileSystemRule();
    private FileSystemAbstraction fs;
    private File file;

    @Before
    public void setUp() throws Exception
    {
        fs = fileSystemRule.get();
        file = testDirectory.file( "ids" );
    }

    @Test
    public void shouldDeleteIfOpen() throws Exception
    {
        // GIVEN
        createEmptyFile();
        IdContainer idContainer = new IdContainer( fs, file, 100, false );
        idContainer.init();

        // WHEN
        idContainer.delete();

        // THEN
        assertFalse( fs.fileExists( file ) );

        idContainer.close( 0 );
    }

    @Test
    public void shouldDeleteIfClosed() throws Exception
    {
        // GIVEN
        createEmptyFile();
        IdContainer idContainer = new IdContainer( fs, file, 100, false );
        idContainer.init();
        idContainer.close( 0 );

        // WHEN
        idContainer.delete();

        // THEN
        assertFalse( fs.fileExists( file ) );
    }

    @Test
    public void shouldForceStickyMark() throws Exception
    {
        // GIVEN
        createEmptyFile();

        // WHEN opening the id generator, where the jvm crashes right after
        IdContainer idContainer = new IdContainer( fs, file, 100, false );
        idContainer.init();

        // THEN
        try
        {
            IdContainer.readHighId( fs, file );
            fail( "Should have thrown, saying something with sticky generator" );
        }
        catch ( InvalidIdGeneratorException e )
        {
            // THEN Good
        }
        finally
        {
            idContainer.close( 0 );
        }
    }

    @Test
    public void shouldTruncateTheFileIfOverwriting() throws Exception
    {
        // GIVEN
        IdContainer.createEmptyIdFile( fs, file, 30, false );
        IdContainer idContainer = new IdContainer( fs, file, 5, false );
        idContainer.init();
        for ( int i = 0; i < 17; i++ )
        {
            idContainer.freeId( i );
        }
        idContainer.close( 30 );
        assertThat( (int) fs.getFileSize( file ), greaterThan( IdContainer.HEADER_SIZE ) );

        // WHEN
        IdContainer.createEmptyIdFile( fs, file, 30, false );

        // THEN
        assertEquals( IdContainer.HEADER_SIZE, (int) fs.getFileSize( file ) );
        assertEquals( 30, IdContainer.readHighId( fs, file ) );
        idContainer = new IdContainer( fs, file, 5, false );
        idContainer.init();
        assertEquals( 30, idContainer.getInitialHighId() );

        idContainer.close( 30 );
    }

    private void createEmptyFile()
    {
        IdContainer.createEmptyIdFile( fs, file, 42, false );
    }
}
