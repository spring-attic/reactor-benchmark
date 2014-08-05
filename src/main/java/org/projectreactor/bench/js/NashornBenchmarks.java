/*
 * Copyright (c) 2011-2014 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectreactor.bench.js;

import jdk.nashorn.api.scripting.JSObject;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.logic.BlackHole;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 */
@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 5, time = 1)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class NashornBenchmarks {

	private ScriptEngine engine;
	private Bindings     bindings;
	private Pojo         pojo;
	private JSObject     hw1;
	private JSObject     hw2;

	@Setup
	public void setup() throws ScriptException {
		engine = new ScriptEngineManager().getEngineByName("nashorn");
		bindings = engine.createBindings();

		pojo = new Pojo();
		bindings.put("pojo", pojo);

		hw1 = (JSObject) engine.eval("function() { return pojo.helloWorld(); }", bindings);
		hw2 = (JSObject) engine.eval("function() { return 'Hello World!'; }", bindings);
	}

	@GenerateMicroBenchmark
	public void javaMethodCallOverhead(BlackHole bh) {
		bh.consume(pojo.helloWorld());
	}

	@GenerateMicroBenchmark
	public void javaScriptEvalOverhead(BlackHole bh) throws ScriptException {
		bh.consume(engine.eval("pojo.helloWorld()", bindings));
	}

	@GenerateMicroBenchmark
	public void wrappedJavaScriptMethodCallOverhead(BlackHole bh) throws ScriptException {
		bh.consume(hw1.call(bindings));
	}

	@GenerateMicroBenchmark
	public void directJavaScriptMethodCallOverhead(BlackHole bh) throws ScriptException {
		bh.consume(hw2.call(bindings));
	}

	public static class Pojo {
		public String helloWorld() {
			return "Hello World!";
		}
	}

}
