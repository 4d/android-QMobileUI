/*
 * Created by Quentin Marciset on 19/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.qmobile.qmobileapi.model.manifest.Manifest
import com.qmobile.qmobileapi.model.manifest.Team
import com.qmobile.qmobileapi.utils.parseJsonToType
import org.json.JSONObject

/**
 * Utility class to get information about the app, the device, etc
 */
object ApplicationUtils {

    const val APPLICATION_ID = "id"
    const val APPLICATION_NAME = "name"
    const val APPLICATION_VERSION = "version"

    private const val TEAM_ID = "id"
    private const val TEAM_NAME = "name"

    /**
     * Reads manifest file and get it as JSONObject
     */
    fun getManifest(context: Context): JSONObject =
        JSONObject(FileUtils.readContentFromFilePath(context, "appinfo.json"))

    fun getTeamInfo(manifestJson: JSONObject): JSONObject {
        return JSONObject().apply {
            val manifest = Gson().parseJsonToType<Manifest>(manifestJson.toString())
            manifest?.let {
                val decodedTeam = Gson().parseJsonToType<Team>(manifest.team.toString())
                decodedTeam?.TeamID?.let { put(TEAM_ID, decodedTeam.TeamID) }
                decodedTeam?.TeamName?.let { put(TEAM_NAME, decodedTeam.TeamName) }
            }
        }
    }

    fun getGuestLogin(manifestJson: JSONObject): Boolean {
        val manifest = Gson().parseJsonToType<Manifest>(manifestJson.toString())
        manifest?.let {
            return manifest.guestLogin
        }
        throw JsonParseException("Couldn't parse GuestLogin")
    }

    fun getRemoteUrl(manifestJson: JSONObject): String {
        val manifest = Gson().parseJsonToType<Manifest>(manifestJson.toString())
        manifest?.let {
            return manifest.remoteUrl ?: ""
        }
        throw JsonParseException("Couldn't parse RemoteUrl")
    }

    fun getQueries(context: Context): JSONObject =
        JSONObject(FileUtils.readContentFromFilePath(context, "queries.json"))
}
