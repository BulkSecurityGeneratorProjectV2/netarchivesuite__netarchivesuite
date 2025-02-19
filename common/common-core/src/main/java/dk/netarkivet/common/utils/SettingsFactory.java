/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.common.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;

/**
 * Generic class for creating class instances from class names given in settings.
 *
 * @param <T> the object-type returned by this class.
 */
public class SettingsFactory<T> {

    /**
     * Creates a new class of the class given in the settings field.
     * <p>
     * If the loaded class has a getInstance() method that matches the given arguments, that will be called to create
     * the class, otherwise a matching constructor will be called, if it exists. This sequence allows for creating
     * singletons.
     * <p>
     * Due to limitations of the Java Reflection API, the parameters of the getInstance method declared on the loaded
     * class must match the given arguments exactly, without subclassing, interface implementation or unboxing. In
     * particular, since any primitive types are automatically boxed when passed to this method, getInstance() methods
     * with primitive type formal parameters will not be found.
     *
     * @param settingsField A field in the Settings class.
     * @param args The arguments that will be passed to the getInstance method or the constructor. These will also be
     * used to determine which getInstance method or constructor to find.
     * @param <T> the object-type returned by this method.
     * @return A new instance of type T created by calling getInstance() or by invoking a constructor.
     * @throws ArgumentNotValid if settingsField is null or the invoked method or constructor threw an exception.
     * @throws IOFailure if there are unrecoverable errors reflecting upon the class.
     * @throws PermissionDenied if the class or methods cannot be accessed.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T getInstance(String settingsField, Object... args) {
        ArgumentNotValid.checkNotNull(settingsField, "String settingsField");
        String className = Settings.get(settingsField);
        try {
            Class<T> aClass = (Class<T>) Class.forName(className);
            Class[] classArgs = new Class[args.length];
            int i = 0;
            for (Object o : args) {
                classArgs[i] = o.getClass();
                ++i;
            }
            Method m = null;
            try {
                m = aClass.getMethod("getInstance", classArgs);
            } catch (NoSuchMethodException e) {
                // The exception is ignored, as we have an alternative
                // approach in searching for constructors.
                Constructor<T> c = null;
                try {
                    c = aClass.getConstructor(classArgs);
                } catch (NoSuchMethodException e1) {
                    throw new ArgumentNotValid("No suitable getInstance() or" + " constructor for class '" + className
                            + "'", e1);
                }
                try {
                    return c.newInstance(args);
                } catch (InvocationTargetException e1) {
                    throw new ArgumentNotValid("Error creating singleton " + "of class '" + className + "': ",
                            e1.getCause());
                }
            }
            try {
                return (T) m.invoke(null, args);
            } catch (InvocationTargetException e) {
                throw new ArgumentNotValid("Error creating singleton of class '" + className + "': ", e.getCause());
            }
        } catch (IllegalAccessException e) {
            throw new PermissionDenied("Cannot access class '" + className + "' defined by '" + settingsField + "'", e);
        } catch (ClassNotFoundException e) {
            throw new IOFailure("Error finding class '" + className + "' defined by '" + settingsField + "'", e);
        } catch (InstantiationException e) {
            throw new IOFailure("Error while instantiating class '" + className + "' defined by '" + settingsField
                    + "'", e);
        } catch (ClassCastException e) {
            throw new IOFailure("Set class '" + className + "' is of wrong type" + " defined by '" + settingsField
                    + "'", e);
        }
    }

}
