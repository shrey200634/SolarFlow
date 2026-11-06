import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AuthLayout } from '../layouts/AuthLayout'
import { TextField } from '../components/forms/fields'
import { Button } from '../components/common/Button'
import { Alert } from '../components/feedback/Alert'
import { useAuth } from '../features/auth/auth-context'
import { parseError } from '../api/client'

const schema = z.object({
  email: z.string().min(1, 'Enter your email').email('Enter a valid email'),
  password: z.string().min(1, 'Enter your password'),
})
type FormValues = z.infer<typeof schema>

export default function LoginPage() {
  const { login, sessionExpired } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  // On success AuthProvider sets the user and GuestOnly redirects.
  const onSubmit = async (values: FormValues) => {
    setError(null)
    try {
      await login(values.email, values.password)
    } catch (e) {
      const p = parseError(e)
      setError(p.status === 401 ? 'Incorrect email or password.' : p.message)
    }
  }

  return (
    <AuthLayout title="Sign in">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {sessionExpired && !error && <Alert tone="info">Your session expired. Sign in again to continue.</Alert>}
        {error && <Alert tone="error">{error}</Alert>}
        <TextField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <TextField label="Password" type="password" autoComplete="current-password" error={errors.password?.message} {...register('password')} />
        <Button type="submit" className="w-full" loading={isSubmitting}>Sign in</Button>
      </form>
      <p className="mt-5 text-center text-sm text-muted">
        New here? <Link to="/register" className="font-medium text-brand hover:underline">Create an account</Link>
      </p>
    </AuthLayout>
  )
}