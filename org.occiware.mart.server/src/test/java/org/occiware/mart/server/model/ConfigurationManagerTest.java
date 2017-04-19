package org.occiware.mart.server.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by christophe on 15/04/2017.
 */
public class ConfigurationManagerTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetConfigurationForOwner() throws Exception {
        ConfigurationManager.initMart();
        String username = "christophe";
        assertNotNull(ConfigurationManager.getConfigurationForOwner(username));
        ConfigurationManager.useAllExtensionForConfigurationInClasspath(username);
    }

    @Test
    public void testFindUserMixinOnConfiguration() throws Exception {

    }

    @Test
    public void testFindMixinOnExtension() throws Exception {

    }

    @Test
    public void testIsKindAttribute() throws Exception {

    }

    @Test
    public void testFindEntity() throws Exception {

    }

    @Test
    public void testIsEntityExist() throws Exception {

    }

    @Test
    public void testFindKindFromExtension() throws Exception {

    }

    @Test
    public void testFindAllEntities() throws Exception {

    }

    @Test
    public void testFindAllEntitiesOwner() throws Exception {

    }

    @Test
    public void testFindAllEntitiesForCategory() throws Exception {

    }

    @Test
    public void testGetExtensionForAction() throws Exception {

    }

    @Test
    public void testGetExtensionForKind() throws Exception {

    }

    @Test
    public void testGetExtensionForMixin() throws Exception {

    }

    @Test
    public void testCheckIfEntityIsResourceOrLinkFromAttributes() throws Exception {

    }

    @Test
    public void testIsCategoryReferencedOnEntity() throws Exception {

    }

    @Test
    public void testGetActionFromEntityWithActionTerm() throws Exception {

    }

    @Test
    public void testGetActionFromEntityWithActionId() throws Exception {

    }

    @Test
    public void testGetUserMixinFromLocation() throws Exception {

    }

    @Test
    public void testIsMixinTags() throws Exception {

    }

    @Test
    public void testGetLocation() throws Exception {

    }

    @Test
    public void testGetLocation1() throws Exception {

    }

    @Test
    public void testGetLocation2() throws Exception {

    }

    @Test
    public void testGetAllConfigurationKind() throws Exception {

    }

    @Test
    public void testGetAllConfigurationMixins() throws Exception {

    }

    @Test
    public void testFindCategorySchemeTermFromTerm() throws Exception {

    }

    @Test
    public void testGetEntitiesLocation() throws Exception {

    }

    @Test
    public void testFindEntityFromLocation() throws Exception {

    }

    @Test
    public void testAddResourceToConfiguration() throws Exception {

    }

    @Test
    public void testAddLinkToConfiguration() throws Exception {

    }

    @Test
    public void testAddMixinsToEntity() throws Exception {

    }

    @Test
    public void testSaveMixinForEntities() throws Exception {

    }

    @Test
    public void testAddUserMixinOnConfiguration() throws Exception {

    }

    @Test
    public void testUpdateAttributesToEntity() throws Exception {

    }

    @Test
    public void testUpdateVersion() throws Exception {

    }

    @Test
    public void testRemoveOrDissociateFromConfiguration() throws Exception {

    }

    @Test
    public void testDissociateMixinFromEntity() throws Exception {

    }

    @Test
    public void testRemoveEntityAttributes() throws Exception {

    }

    @Test
    public void testRemoveUserMixinFromConfiguration() throws Exception {

    }

    @Test
    public void testApplyFilterOnInterface() throws Exception {

    }
}