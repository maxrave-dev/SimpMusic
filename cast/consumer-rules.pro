# CastOptionsProvider is instantiated by the Cast framework via reflection,
# using the fully-qualified class name declared as a plain string in the
# OPTIONS_PROVIDER_CLASS_NAME manifest meta-data. R8 cannot see that
# reference statically (no direct call site), so without this rule the
# class — or its no-arg constructor — can be renamed/removed by a
# consuming app's release (R8 optimize+obfuscate) build, crashing Cast
# initialization at runtime.
-keep class org.simpmusic.cast.CastOptionsProvider {
    public <init>();
}
