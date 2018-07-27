/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.iaa.utils;

import com.google.gson.*;
import java.lang.reflect.Type;
import jetbrains.buildServer.iaa.common.Responsibility;

public class ResponsibilitySerializer implements JsonSerializer<Responsibility> {

  @Override
  public JsonElement serialize(final Responsibility responsibility,
                               final Type typeOfSrc,
                               final JsonSerializationContext context) {
    JsonObject result = new JsonObject();
    result.add("Supposed investigator", new JsonPrimitive(responsibility.getUser().getDescriptiveName()));
    result.add("Description", new JsonPrimitive(responsibility.getDescription()));
    return result;
  }
}
