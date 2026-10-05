# Settings store enum constants by name and read them back with valueOf,
# so their names must survive obfuscation and stay the same between releases.
-keepclassmembers enum com.anils.sarjmetre.settings.** {
    <fields>;
}
