#version 330 core

out vec4 FragColor;

uniform vec3 u_CameraPos;
uniform vec3 u_LightPos;
uniform float u_Time;
uniform vec2 u_Resolution;
uniform vec3 u_Forward;
uniform vec3 u_Right;
uniform vec3 u_Up;

// Capped Torus SDF (Inigo Quilez)
float sdCappedTorus(in vec3 p, in vec2 sc, in float ra, in float rb) {
    p.x = abs(p.x);
    float k = (sc.y * p.x > sc.x * p.y) ? dot(p.xy, sc) : length(p.xy);
    return sqrt(dot(p, p) + ra * ra - 2.0 * ra * k) - rb;
}

float sdSphere( vec3 p, float s )
{
    return length(p)-s;
}

float smin( float a, float b, float k )
{
    k *= log(2.0);
    float x = b-a;
    return a + x/(1.0-exp2(x/k));
}

float sub( float d1, float d2 )
{
    return max(-d1,d2);
}

float sdBox( vec3 p, vec3 b )
{
    vec3 q = abs(p) - b;
    return length(max(q,0.0)) + min(max(q.x,max(q.y,q.z)),0.0);
}

float sdPlane( vec3 p, vec3 n, float h )
{
    // n must be normalized
    return dot(p,n) + h;
}

float map(in vec3 pos, in vec3 rd) {
    float spacing = 1;
    vec3 q = pos;
    q.xyz = mod(q.xyz + spacing * 0.5, spacing) - spacing * 0.5;
    float an = 2.5 * (0.5 + 0.5 * sin(u_Time * 1.1 + 3.0));
    vec2 c = vec2(sin(an), cos(an));
//    float torusDist = sdCappedTorus(pos, c, 0.4, 0.1);
    float torusDist = sdSphere(q - vec3(0, -0, 0), 0.7);
    float boxDist = sdBox(q - vec3(0, -0, 0), vec3(0.5, 0.5, 0.5));
    float planeDist = sdPlane(pos, vec3(0, 1, 0), 0.5); // плоскость y = -1, h=1 (dot(p,n)+h=0 -> y=-h)
    return sub(torusDist, boxDist);
}

vec3 calcNormal(in vec3 pos, in vec3 rd) {
    vec2 e = vec2(1.0, -1.0) * 0.5773;
    const float eps = 0.0005;
    return normalize(
    e.xyy * map(pos + e.xyy * eps, rd) +
    e.yyx * map(pos + e.yyx * eps, rd) +
    e.yxy * map(pos + e.yxy * eps, rd) +
    e.xxx * map(pos + e.xxx * eps, rd)
    );
}

void main() {
    // Convert fragment coords to [-1, 1]
    vec2 uv = (gl_FragCoord.xy / u_Resolution) * 2.0 - 1.0;
    uv.x *= u_Resolution.x / u_Resolution.y;

    // Construct view ray from camera basis
    vec3 ro = u_CameraPos;
    vec3 rd = normalize(uv.x * u_Right + uv.y * u_Up + 1.5 * u_Forward);
//    vec3 rd = vec3(vec3(1, 0, 0) * uv.x + vec3(0, 1, 0) * uv.y + vec3(0, 0, 1));

    // Raymarching
    const float tmax = 120.0;
    float t = 0.0;
    float dist;
    for (int i = 0; i < 128; i++) {
        vec3 pos = ro + rd * t;
        dist = map(pos, rd);
        if (dist < 0.001 || t > tmax) break;
        t += dist;
    }

    vec3 fogColor = vec3(169.0 / 255.0, 169.0 / 255.0, 169.0 / 255.0); // or any color you want
    float fogDensity = 0.1; // higher = more fog

    vec3 col = vec3(161.0 / 255.0, 227.0 / 255.0, 249.0 / 255.0);
    if (t < tmax) {
        float fogFactor = exp(-t * fogDensity);
        fogFactor = clamp(fogFactor, 0.0, 1.0);

        vec3 def = vec3(165.0 / 255.0, 198.0 / 255.0, 177.0 / 255.0);

        vec3 pos = ro + rd * t;
        vec3 nor = calcNormal(pos, rd);
        vec3 lightDir = normalize(u_LightPos - pos);

        float diff = clamp(dot(nor, lightDir), 0.0, 1.0);
        float amb = 0.2;

        vec3 viewDir = normalize(u_CameraPos - pos);
        vec3 reflectDir = reflect(-lightDir, nor);
        float spec = pow(max(dot(viewDir, reflectDir), 0.0), 512.0); // "64.0" — степень блика

        vec3 ambient = vec3(0.1);
        vec3 diffuse = vec3(0.8, 0.7, 0.5) * diff;
        vec3 specular = vec3(1.0) * spec;

        col = ambient + diffuse;

        col = mix(fogColor, col, fogFactor);
        col = col + specular;
//        float ao = clamp((map(pos + nor * 0.02, rd) - map(pos, rd)) * 5.0, 0.0, 1.0);
//        col *= ao;
        col = pow( col, vec3(0.4545) );
        col = mix(col, def, 0.7);
    }



    FragColor = vec4(col, 1.0);

}
