import React, { useEffect, useRef, useState } from 'react'
import * as THREE from 'three'

interface TuxCanvasProps {
  className?: string
}

const checkWebGlSupport = (): boolean => {
  if (typeof window === 'undefined') return false
  try {
    const canvas = document.createElement('canvas')
    return Boolean(canvas.getContext('webgl') || canvas.getContext('experimental-webgl'))
  } catch {
    return false
  }
}

export const TuxCanvas: React.FC<TuxCanvasProps> = ({ className = '' }) => {
  const containerRef = useRef<HTMLDivElement>(null)
  const [webGlSupported] = useState<boolean>(() => checkWebGlSupport())
  const [isInteracting, setIsInteracting] = useState(false)

  useEffect(() => {
    if (!webGlSupported) return

    const container = containerRef.current
    if (!container) return

    // Check for reduced motion preference
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

    // 2. Scene, Camera, Renderer Setup
    const scene = new THREE.Scene()
    scene.background = null // Transparent background

    const width = container.clientWidth || 400
    const height = container.clientHeight || 400

    const camera = new THREE.PerspectiveCamera(40, width / height, 0.1, 100)
    camera.position.set(0, 0.2, 4.2)
    camera.lookAt(0, 0, 0)

    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true,
      powerPreference: 'high-performance',
    })
    renderer.setSize(width, height)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.shadowMap.enabled = true
    renderer.shadowMap.type = THREE.PCFSoftShadowMap

    container.appendChild(renderer.domElement)

    // 3. Materials
    const blackBodyMat = new THREE.MeshStandardMaterial({
      color: 0x111111,
      roughness: 0.35,
      metalness: 0.15,
    })

    const whiteBellyMat = new THREE.MeshStandardMaterial({
      color: 0xf5f5f3,
      roughness: 0.4,
      metalness: 0.05,
    })

    const orangeBeakMat = new THREE.MeshStandardMaterial({
      color: 0xff8c00,
      roughness: 0.3,
      metalness: 0.1,
    })

    const eyeWhiteMat = new THREE.MeshStandardMaterial({
      color: 0xffffff,
      roughness: 0.2,
    })

    const pupilMat = new THREE.MeshBasicMaterial({
      color: 0x050505,
    })

    // 4. Procedural Tux Group Assembly
    const tuxGroup = new THREE.Group()
    scene.add(tuxGroup)

    // Torso / Body (Sculpted egg-shaped cylinder/sphere)
    const bodyGeo = new THREE.SphereGeometry(0.85, 32, 32)
    bodyGeo.scale(0.9, 1.15, 0.8)
    const bodyMesh = new THREE.Mesh(bodyGeo, blackBodyMat)
    bodyMesh.castShadow = true
    tuxGroup.add(bodyMesh)

    // Head
    const headGeo = new THREE.SphereGeometry(0.62, 32, 32)
    headGeo.scale(0.95, 0.9, 0.9)
    const headMesh = new THREE.Mesh(headGeo, blackBodyMat)
    headMesh.position.set(0, 0.9, 0.05)
    tuxGroup.add(headMesh)

    // White Belly Patch
    const bellyGeo = new THREE.SphereGeometry(0.72, 32, 32)
    bellyGeo.scale(0.75, 0.95, 0.4)
    const bellyMesh = new THREE.Mesh(bellyGeo, whiteBellyMat)
    bellyMesh.position.set(0, -0.05, 0.45)
    tuxGroup.add(bellyMesh)

    // Eyes (Left & Right)
    const eyeGeo = new THREE.SphereGeometry(0.12, 16, 16)
    eyeGeo.scale(0.8, 1.1, 0.5)

    const leftEye = new THREE.Mesh(eyeGeo, eyeWhiteMat)
    leftEye.position.set(-0.2, 1.02, 0.52)
    leftEye.rotation.y = -0.15
    tuxGroup.add(leftEye)

    const rightEye = new THREE.Mesh(eyeGeo, eyeWhiteMat)
    rightEye.position.set(0.2, 1.02, 0.52)
    rightEye.rotation.y = 0.15
    tuxGroup.add(rightEye)

    // Pupils
    const pupilGeo = new THREE.SphereGeometry(0.05, 12, 12)
    const leftPupil = new THREE.Mesh(pupilGeo, pupilMat)
    leftPupil.position.set(-0.19, 1.02, 0.58)
    tuxGroup.add(leftPupil)

    const rightPupil = new THREE.Mesh(pupilGeo, pupilMat)
    rightPupil.position.set(0.19, 1.02, 0.58)
    tuxGroup.add(rightPupil)

    // Beak
    const beakGeo = new THREE.ConeGeometry(0.2, 0.38, 16)
    beakGeo.rotateX(Math.PI / 2)
    beakGeo.scale(1.2, 0.6, 1)
    const beakMesh = new THREE.Mesh(beakGeo, orangeBeakMat)
    beakMesh.position.set(0, 0.82, 0.64)
    tuxGroup.add(beakMesh)

    // Flippers / Wings
    const flipperGeo = new THREE.SphereGeometry(0.4, 24, 24)
    flipperGeo.scale(0.25, 1.1, 0.55)

    const leftFlipper = new THREE.Mesh(flipperGeo, blackBodyMat)
    leftFlipper.position.set(-0.85, 0.1, 0)
    leftFlipper.rotation.z = 0.35
    leftFlipper.rotation.y = -0.2
    tuxGroup.add(leftFlipper)

    const rightFlipper = new THREE.Mesh(flipperGeo, blackBodyMat)
    rightFlipper.position.set(0.85, 0.1, 0)
    rightFlipper.rotation.z = -0.35
    rightFlipper.rotation.y = 0.2
    tuxGroup.add(rightFlipper)

    // Feet
    const footGeo = new THREE.SphereGeometry(0.3, 16, 16)
    footGeo.scale(1.1, 0.25, 1.4)

    const leftFoot = new THREE.Mesh(footGeo, orangeBeakMat)
    leftFoot.position.set(-0.4, -0.95, 0.3)
    leftFoot.rotation.y = -0.25
    tuxGroup.add(leftFoot)

    const rightFoot = new THREE.Mesh(footGeo, orangeBeakMat)
    rightFoot.position.set(0.4, -0.95, 0.3)
    rightFoot.rotation.y = 0.25
    tuxGroup.add(rightFoot)

    // Ground Soft Shadow Disc
    const shadowGeo = new THREE.CircleGeometry(1.1, 32)
    const shadowMat = new THREE.MeshBasicMaterial({
      color: 0x000000,
      transparent: true,
      opacity: 0.35,
    })
    const shadowMesh = new THREE.Mesh(shadowGeo, shadowMat)
    shadowMesh.rotation.x = -Math.PI / 2
    shadowMesh.position.set(0, -1.05, 0)
    scene.add(shadowMesh)

    // 5. Lighting
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.9)
    scene.add(ambientLight)

    const keyLight = new THREE.DirectionalLight(0xffffff, 1.6)
    keyLight.position.set(3, 4, 4)
    keyLight.castShadow = true
    scene.add(keyLight)

    // Subtle lime cyber-accent rim light
    const limeRimLight = new THREE.DirectionalLight(0xa3e635, 0.6)
    limeRimLight.position.set(-3, -1, -2)
    scene.add(limeRimLight)

    // Soft frontal fill
    const fillLight = new THREE.PointLight(0xffffff, 0.5, 10)
    fillLight.position.set(0, 0, 3)
    scene.add(fillLight)

    // 6. Interactive Cursor Tracking & Animation Loop
    let targetRotY = 0
    let targetRotX = 0
    let currentRotY = 0
    let currentRotX = 0

    const handleMouseMove = (event: MouseEvent) => {
      if (prefersReducedMotion) return
      const rect = container.getBoundingClientRect()
      const x = ((event.clientX - rect.left) / rect.width) * 2 - 1
      const y = -(((event.clientY - rect.top) / rect.height) * 2 - 1)

      // Clamp max tilt angles
      targetRotY = THREE.MathUtils.clamp(x * 0.55, -0.6, 0.6)
      targetRotX = THREE.MathUtils.clamp(-y * 0.25, -0.3, 0.3)
    }

    const handleMouseLeave = () => {
      targetRotY = 0
      targetRotX = 0
      setIsInteracting(false)
    }

    const handleMouseEnter = () => {
      setIsInteracting(true)
    }

    window.addEventListener('mousemove', handleMouseMove)
    container.addEventListener('mouseenter', handleMouseEnter)
    container.addEventListener('mouseleave', handleMouseLeave)

    // Handle Resize
    const handleResize = () => {
      if (!container) return
      const newWidth = container.clientWidth
      const newHeight = container.clientHeight
      camera.aspect = newWidth / newHeight
      camera.updateProjectionMatrix()
      renderer.setSize(newWidth, newHeight)
    }

    window.addEventListener('resize', handleResize)

    // Animation Frame Loop
    let animationFrameId: number
    const clock = new THREE.Clock()

    const animate = () => {
      animationFrameId = requestAnimationFrame(animate)

      const elapsedTime = clock.getElapsedTime()

      if (!prefersReducedMotion) {
        // Idle gentle levitation
        tuxGroup.position.y = Math.sin(elapsedTime * 1.6) * 0.06
        // Shadow breathes with height
        shadowMesh.scale.setScalar(1 - Math.sin(elapsedTime * 1.6) * 0.05)

        // Smooth damping interpolation towards cursor target
        currentRotY += (targetRotY - currentRotY) * 0.06
        currentRotX += (targetRotX - currentRotX) * 0.06

        tuxGroup.rotation.y = currentRotY + Math.sin(elapsedTime * 0.7) * 0.04
        tuxGroup.rotation.x = currentRotX
      }

      renderer.render(scene, camera)
    }

    animate()

    // 7. Proper Cleanup on Unmount
    return () => {
      cancelAnimationFrame(animationFrameId)
      window.removeEventListener('mousemove', handleMouseMove)
      window.removeEventListener('resize', handleResize)
      container.removeEventListener('mouseenter', handleMouseEnter)
      container.removeEventListener('mouseleave', handleMouseLeave)

      // Dispose Geometries & Materials
      const disposeNode = (node: THREE.Object3D) => {
        if ((node as THREE.Mesh).isMesh) {
          const mesh = node as THREE.Mesh
          mesh.geometry?.dispose()
          if (Array.isArray(mesh.material)) {
            mesh.material.forEach((m) => m.dispose())
          } else {
            mesh.material?.dispose()
          }
        }
      }

      scene.traverse(disposeNode)
      renderer.dispose()

      if (renderer.domElement && container.contains(renderer.domElement)) {
        container.removeChild(renderer.domElement)
      }
    }
  }, [webGlSupported])

  return (
    <div className={`tux-viewport-wrapper ${className}`}>
      {/* Editorial System Identifier Top Bar */}
      <div className="tux-viewport-header">
        <span>SYS // TUX_3D_CORE</span>
        <span style={{ color: isInteracting ? 'var(--lime-accent)' : 'inherit' }}>
          {isInteracting ? 'PARALLAX : TRACKING' : 'MODE : FLOATING'}
        </span>
      </div>

      {/* 3D Canvas or Fallback */}
      {webGlSupported ? (
        <div ref={containerRef} className="tux-canvas-container" title="Interactive 3D Tux Penguin" />
      ) : (
        <div
          style={{
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 32,
            textAlign: 'center',
          }}
        >
          <img
            src="/assets/won.png"
            alt="MIT Tech Kernel Tux Mascot"
            style={{ width: 120, height: 120, opacity: 0.9, marginBottom: 16 }}
          />
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--kernel-text-dark-muted)' }}>
            [WEBGL HARDWARE ACCELERATION UNAVAILABLE]
            <br />
            DISPLAYING STATIC EMBLEM FALLBACK
          </p>
        </div>
      )}

      {/* Editorial Status Strip Bottom Bar */}
      <div className="tux-viewport-footer">
        <span>RENDER : THREE.JS PBR</span>
        <span>STATUS : NOMINAL</span>
      </div>
    </div>
  )
}
export default TuxCanvas
