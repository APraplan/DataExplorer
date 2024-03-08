/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2017,2018,2019 Thomas Eickert
****************************************************************************************/
/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gde.histo.cache;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import com.google.gson.Gson;

import gde.DataAccess;
import gde.exception.ThrowableUtils;
import gde.log.Level;
import gde.log.Logger;

/**
 * Vaults IO operations with marshaller / unmarshaller caching.
 * @author Thomas Eickert
 */
public final class VaultProxy {
	private static final String	$CLASS_NAME				= VaultProxy.class.getName();
	private static final Logger	log								= Logger.getLogger($CLASS_NAME);

	private static Schema				vaultSchema;

	private static Unmarshaller	jaxbUnmarshaller	= null;
	private static Marshaller		jaxbMarshaller		= null;

	private static Schema getVaultSchema() {
		if (vaultSchema == null) {
			try (InputStream inputStream = DataAccess.getInstance().getCacheXsdInputStream()) { // ok
				StreamSource xsdStreamSource = new StreamSource(inputStream);
				vaultSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsdStreamSource);
			} catch (Exception e) {
				throw ThrowableUtils.rethrow(e);
			}
		}
		return vaultSchema;
	}

	/**
	 * @return cached instance (unmarshaller is not thread safe) which is ~100 ms faster than creating a new instance from a cached JaxbContext instance
	 */
	public static Unmarshaller getUnmarshaller() {
		if (jaxbUnmarshaller == null) {
			try {
				jaxbUnmarshaller = HistoVault.getJaxbContext().createUnmarshaller();
				jaxbUnmarshaller.setSchema(getVaultSchema());
			} catch (Exception e) {
				throw ThrowableUtils.rethrow(e);
			}
		}
		return jaxbUnmarshaller;
	}

	/**
	 * @return cached instance (marshaller is not thread safe) which is ~100 ms faster than creating a new instance from a cached JaxbContext instance
	 */
	public static Marshaller getMarshaller() {
		if (jaxbMarshaller == null) {
			try {
				jaxbMarshaller = HistoVault.getJaxbContext().createMarshaller();
				jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				jaxbMarshaller.setSchema(getVaultSchema());
			} catch (Exception e) {
				throw ThrowableUtils.rethrow(e);
			}
		}
		return jaxbMarshaller;
	}

	/**
	 * Threadsafe loading in JSON format without closing the stream.
	 * @param inputStream is a stream to the source path
	 * @return the vault
	 */
	public static HistoVault loadJson(InputStream inputStream) {
		HistoVault vault = null;
		try {
			Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			vault = new Gson().fromJson(reader, HistoVault.class);
		} catch (Exception e) {
			ThrowableUtils.rethrow(e);
		}
		return vault;
	}

	/**
	 * @param inputStream is a stream to the source path
	 * @return the vault
	 */
	public static HistoVault load(InputStream inputStream) {
		HistoVault vault = null;
		try {
			vault = (HistoVault) getUnmarshaller().unmarshal(inputStream);
		} catch (Exception e) {
			ThrowableUtils.rethrow(e);
		}
		return vault;
	}

	/**
	 * Threadsafe storage in JSON format without closing the stream.
	 * @param newVault is the vault to be stored
	 * @param outputStream is a stream to the target path
	 */
	public static void storeJson(HistoVault newVault, OutputStream outputStream) {
		HistoVault storableVault = newVault instanceof ExtendedVault ? new HistoVault(newVault) : newVault;
		String json = new Gson().toJson(storableVault);
		log.log(Level.FINER, json);
		try {
			// no buffered writer as we invoke the write method only once
			OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
			writer.write(json);
			writer.flush();
		} catch (Exception e) {
			ThrowableUtils.rethrow(e);
		}
	}

	/**
	 * @param newVault is the vault to be stored
	 * @param outputStream is a stream to the target path
	 */
	public static void store(HistoVault newVault, OutputStream outputStream) {
		try {
			getMarshaller().marshal(newVault, outputStream);
		} catch (Exception e) {
			ThrowableUtils.rethrow(e);
		}
	}

	/**
	 * @param newVault is the vault to be stored
	 * @param outputStream is a stream to the target path
	 * @throws JAXBException
	 */
	public static void threadSafeStore(HistoVault newVault, OutputStream outputStream) throws JAXBException {
		Marshaller marshaller = HistoVault.getJaxbContext().createMarshaller();
		marshaller.setSchema(getVaultSchema());
		marshaller.marshal(newVault, outputStream);
	}

	/**
	 * This load method takes 650 to 1261 ms for 137 vaults compared to 176 to 268 ms with the static load method.
	 * @param inputStream is a stream to the source path
	 * @return the vault
	 */
	@SuppressWarnings("static-method") // JAXB marshallers are not threadsafe
	public HistoVault threadSafeLoad(InputStream inputStream) throws JAXBException {
		Unmarshaller unmarshaller = HistoVault.getJaxbContext().createUnmarshaller();
		unmarshaller.setSchema(getVaultSchema());
		return (HistoVault) unmarshaller.unmarshal(inputStream);
	}

}
