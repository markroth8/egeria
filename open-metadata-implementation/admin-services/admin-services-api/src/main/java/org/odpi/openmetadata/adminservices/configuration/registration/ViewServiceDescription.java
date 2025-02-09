/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.adminservices.configuration.registration;

import java.io.Serializable;

/**
 * ViewServiceDescription provides a list of registered view services.
 */
public enum ViewServiceDescription implements Serializable
{
    GLOSSARY_AUTHOR(800,
                    "Glossary Author",
                    "Glossary Author OMVS",
                    "glossary-author",
                    "View Service for glossary authoring.",
                    "https://odpi.github.io/egeria-docs/services/omvs/glossary-author/overview"),

    REPOSITORY_EXPLORER(801,
                  "Repository Explorer",
                  "Repository Explorer OMVS",
                  "rex",
                  "Explore open metadata instances.",
                  "https://odpi.github.io/egeria-docs/services/omvs/rex/overview"),

    TYPE_EXPLORER(802,
                  "Type Explorer",
                  "Type Explorer OMVS",
                  "tex",
                  "Explore the open metadata types in a repository or cohort.",
                  "https://odpi.github.io/egeria-docs/services/omvs/tex/overview"),

    DINO(803,
                  "Dynamic Infrastructure and Operations",
                  "Dynamic Infrastructure and Operations OMVS",
                  "dino",
                  "Explore and operate an open metadata ecosystem.",
                  "https://odpi.github.io/egeria-docs/services/omvs/dino/overview"),

    SERVER_AUTHOR(804,
                 "Server Author",
                 "Server Author OMVS",
                 "server-author",
                 "Author servers.",
                 "https://odpi.github.io/egeria-docs/services/omvs/server-author/overview/");

    private static final long serialVersionUID = 1L;

    private int    viewServiceCode;
    private String viewServiceName;
    private String viewServiceFullName;
    private String viewServiceURLMarker;
    private String viewServiceDescription;
    private String viewServiceWiki;


    /**
     * Default Constructor
     *
     * @param viewServiceCode        ordinal for this UI view
     * @param viewServiceURLMarker   string used in URLs
     * @param viewServiceName        symbolic name for this UI view
     * @param viewServiceDescription short description for this UI view
     * @param viewServiceWiki        wiki page for the UI view for this UI view
     */
    ViewServiceDescription(int    viewServiceCode,
                           String viewServiceName,
                           String viewServiceFullName,
                           String viewServiceURLMarker,
                           String viewServiceDescription,
                           String viewServiceWiki)
    {
        /*
         * Save the values supplied
         */
        this.viewServiceCode = viewServiceCode;
        this.viewServiceName = viewServiceName;
        this.viewServiceFullName = viewServiceFullName;
        this.viewServiceURLMarker = viewServiceURLMarker;
        this.viewServiceDescription = viewServiceDescription;
        this.viewServiceWiki = viewServiceWiki;
    }


    /**
     * Return the code for this enum instance
     *
     * @return int type code
     */
    public int getViewServiceCode() {
        return viewServiceCode;
    }


    /**
     * Return the default name for this enum instance.
     *
     * @return String default name
     */
    public String getViewServiceName() {
        return viewServiceName;
    }


    /**
     * Return the formal name for this enum instance.
     *
     * @return String default name
     */
    public String getViewServiceFullName() {
        return viewServiceFullName;
    }

    /**
     * Return the string that appears in the REST API URL that identifies the owning service.
     * Null means no REST APIs supported by this service.
     *
     * @return String default name
     */
    public String getViewServiceURLMarker() {
        return viewServiceURLMarker;
    }

    /**
     * Return the default description for the type for this enum instance.
     *
     * @return String default description
     */
    public String getViewServiceDescription() {
        return viewServiceDescription;
    }


    /**
     * Return the URL for the wiki page describing this UI view.
     *
     * @return String URL name for the wiki page
     */
    public String getViewServiceWiki() {
        return viewServiceWiki;
    }


}
